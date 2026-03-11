package com.frauddetection.service;

import com.frauddetection.domain.FraudDecision;
import com.frauddetection.domain.Transaction;
import com.frauddetection.dto.FraudResponse;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dolandırıcılık tespit orkestratör servisi.
 * 
 * Bu servis, dolandırıcılık tespit sürecini koordine eder:
 * 1. İşlemi alır
 * 2. Risk skorunu hesaplar (RiskScoreCalculator kullanarak)
 * 3. Karar verir (APPROVE, REVIEW, BLOCK)
 * 4. Metrikleri günceller
 * 5. Yanıt oluşturur
 * 
 * Orchestrator pattern kullanılarak, iş mantığı modüler kurallara
 * ayrılmıştır ve bu servis sadece koordinasyonu sağlar.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@Service
public class FraudDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    
    // Risk skoru hesaplayıcı
    private final RiskScoreCalculator riskScoreCalculator;
    
    // Persistence servisi (optional - null olabilir)
    private final TransactionPersistenceService persistenceService;
    
    // Karar eşikleri
    private final BigDecimal approveMaxThreshold;
    private final BigDecimal reviewMinThreshold;
    private final BigDecimal reviewMaxThreshold;
    private final BigDecimal blockMinThreshold;
    
    // Metrik sayaçları
    private final Counter approveCounter;
    private final Counter reviewCounter;
    private final Counter blockCounter;
    private final Counter totalTransactionsCounter;
    
    /**
     * Constructor - bağımlılıkları enjekte eder.
     * 
     * @param riskScoreCalculator Risk skoru hesaplayıcı
     * @param meterRegistry Metrik kayıt defteri
     * @param persistenceService Persistence servisi (optional)
     * @param approveMaxThreshold Onay için maksimum eşik
     * @param reviewMinThreshold İnceleme için minimum eşik
     * @param reviewMaxThreshold İnceleme için maksimum eşik
     * @param blockMinThreshold Engelleme için minimum eşik
     */
    @Autowired
    public FraudDetectionService(
            RiskScoreCalculator riskScoreCalculator,
            MeterRegistry meterRegistry,
            @Autowired(required = false) TransactionPersistenceService persistenceService,
            @Value("${fraud.detection.thresholds.approve-max:0.4}") BigDecimal approveMaxThreshold,
            @Value("${fraud.detection.thresholds.review-min:0.4}") BigDecimal reviewMinThreshold,
            @Value("${fraud.detection.thresholds.review-max:0.7}") BigDecimal reviewMaxThreshold,
            @Value("${fraud.detection.thresholds.block-min:0.7}") BigDecimal blockMinThreshold) {
        
        this.riskScoreCalculator = riskScoreCalculator;
        this.persistenceService = persistenceService;
        this.approveMaxThreshold = approveMaxThreshold;
        this.reviewMinThreshold = reviewMinThreshold;
        this.reviewMaxThreshold = reviewMaxThreshold;
        this.blockMinThreshold = blockMinThreshold;
        
        // Metrikleri başlat
        this.approveCounter = Counter.builder("fraud.decision.approve")
                .description("Onaylanan işlem sayısı")
                .register(meterRegistry);
        this.reviewCounter = Counter.builder("fraud.decision.review")
                .description("İnceleme gerektiren işlem sayısı")
                .register(meterRegistry);
        this.blockCounter = Counter.builder("fraud.decision.block")
                .description("Engellenen işlem sayısı")
                .register(meterRegistry);
        this.totalTransactionsCounter = Counter.builder("fraud.transactions.total")
                .description("Analiz edilen toplam işlem sayısı")
                .register(meterRegistry);
        
        logger.info("FraudDetectionService başlatıldı - Eşikler: approve<{}, review={}-{}, block>{}",
                approveMaxThreshold, reviewMinThreshold, reviewMaxThreshold, blockMinThreshold);
    }

    /**
     * Testler veya manuel kullanım için persistence servisi olmadan kolayca
     * instantiation yapabilmek adına ek bir constructor.
     */
    public FraudDetectionService(
            RiskScoreCalculator riskScoreCalculator,
            MeterRegistry meterRegistry,
            BigDecimal approveMaxThreshold,
            BigDecimal reviewMinThreshold,
            BigDecimal reviewMaxThreshold,
            BigDecimal blockMinThreshold) {
        this(riskScoreCalculator, meterRegistry, null,
                approveMaxThreshold, reviewMinThreshold, reviewMaxThreshold, blockMinThreshold);
    }
    
    /**
     * İşlemi analiz eder ve dolandırıcılık kararı verir.
     * 
     * @param transaction Analiz edilecek işlem
     * @return Dolandırıcılık analiz sonucu
     */
    @Timed(value = "fraud.analysis.duration", description = "Dolandırıcılık analizi için geçen süre")
    @org.springframework.transaction.annotation.Transactional
    public FraudResponse analyzeTransaction(Transaction transaction) {
        logger.debug("İşlem analiz ediliyor: {}", transaction.getTransactionId());
        
        // Risk skorunu hesapla
        BigDecimal riskScore = riskScoreCalculator.calculateFinalRiskScore(transaction);
        
        // Kararı belirle
        FraudDecision decision = determineDecision(riskScore);
        
        // Risk açıklamalarını al
        List<String> riskReasons = riskScoreCalculator.getRiskReasons(transaction);
        String reason = riskReasons.isEmpty() 
                ? "Şüpheli desen tespit edilmedi" 
                : String.join("; ", riskReasons);
        
        // Metrikleri güncelle
        updateMetrics(decision);
        
        // Yanıt oluştur
        FraudResponse response = new FraudResponse(
                transaction.getTransactionId(),
                decision,
                riskScore,
                decision.getDescription(),
                reason
        );
        
        // Persistence katmanına kaydet (hata olsa bile fraud detection devam eder)
        if (persistenceService != null) {
            try {
                persistenceService.saveFraudAnalysisResult(transaction, response);
            } catch (Exception e) {
                logger.warn("İşlem veritabanına kaydedilemedi (fraud detection devam ediyor): {}", 
                           transaction.getTransactionId(), e);
            }
        }
        
        logger.info("İşlem {} analiz edildi - Karar: {}, Risk Skoru: {}", 
                   transaction.getTransactionId(), decision, riskScore);
        
        return response;
    }
    
    /**
     * Risk skoruna göre dolandırıcılık kararını belirler.
     * 
     * @param riskScore Hesaplanan risk skoru
     * @return FraudDecision (APPROVE, REVIEW veya BLOCK)
     */
    private FraudDecision determineDecision(BigDecimal riskScore) {
        if (riskScore.compareTo(blockMinThreshold) >= 0) {
            return FraudDecision.BLOCK;
        } else if (riskScore.compareTo(reviewMinThreshold) >= 0) {
            return FraudDecision.REVIEW;
        } else {
            return FraudDecision.APPROVE;
        }
    }
    
    /**
     * Metrikleri günceller.
     * 
     * @param decision Verilen karar
     */
    private void updateMetrics(FraudDecision decision) {
        totalTransactionsCounter.increment();
        
        switch (decision) {
            case APPROVE -> approveCounter.increment();
            case REVIEW -> reviewCounter.increment();
            case BLOCK -> blockCounter.increment();
        }
    }
}

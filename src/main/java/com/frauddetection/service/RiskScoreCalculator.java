package com.frauddetection.service;

import com.frauddetection.domain.Transaction;
import com.frauddetection.service.rules.FraudRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Risk skoru hesaplama servisi.
 * 
 * Tüm fraud kurallarını toplayarak nihai risk skorunu hesaplar.
 * Her kuralın ağırlığı yapılandırılabilir ve kurallar birbirinden
 * bağımsız olarak çalışır.
 * 
 * Risk skoru hesaplama mantığı:
 * 1. Her kuraldan risk skoru alınır (0.0 - 1.0)
 * 2. Her kuralın ağırlığı ile çarpılır
 * 3. Tüm ağırlıklı skorlar toplanır
 * 4. Sonuç 0.0 - 1.0 aralığına normalize edilir
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@Service
public class RiskScoreCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskScoreCalculator.class);
    
    // Tüm fraud kuralları - Spring tarafından otomatik enjekte edilir
    private final List<FraudRule> fraudRules;
    
    // Kural ağırlıkları - config'den okunur
    private final Map<String, BigDecimal> ruleWeights;
    
    // Risk skoru sınırları
    private static final BigDecimal MIN_RISK_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_RISK_SCORE = BigDecimal.ONE;
    private static final int RISK_SCORE_SCALE = 4; // Ondalık basamak sayısı
    
    /**
     * Constructor - tüm fraud kurallarını ve ağırlıkları enjekte eder.
     * 
     * @param fraudRules Tüm FraudRule implementasyonları (Spring tarafından otomatik toplanır)
     * @param timeOfDayWeight Gün saati kuralı ağırlığı
     * @param amountWeight Tutar kuralı ağırlığı
     * @param frequencyWeight Sıklık kuralı ağırlığı
     */
    public RiskScoreCalculator(
            List<FraudRule> fraudRules,
            @Value("${fraud.detection.rules.time-of-day-weight:0.30}") BigDecimal timeOfDayWeight,
            @Value("${fraud.detection.rules.amount-weight:0.35}") BigDecimal amountWeight,
            @Value("${fraud.detection.rules.frequency-weight:0.35}") BigDecimal frequencyWeight) {
        
        this.fraudRules = fraudRules;
        
        // Kural ağırlıklarını map'e dönüştür
        this.ruleWeights = Map.of(
                "Gün Saati Kuralı", timeOfDayWeight,
                "Tutar Kuralı", amountWeight,
                "Sıklık Kuralı", frequencyWeight
        );
        
        logger.info("RiskScoreCalculator başlatıldı - {} kural yüklendi", fraudRules.size());
    }
    
    /**
     * İşlem için nihai risk skorunu hesaplar.
     * 
     * @param transaction Analiz edilecek işlem
     * @return 0.0 (düşük risk) ile 1.0 (yüksek risk) arasında normalize edilmiş risk skoru
     */
    public BigDecimal calculateFinalRiskScore(Transaction transaction) {
        if (transaction == null) {
            logger.warn("Geçersiz işlem - risk skoru 0.0 döndürülüyor");
            return MIN_RISK_SCORE;
        }
        
        BigDecimal totalWeightedScore = BigDecimal.ZERO;
        
        // Her kuraldan risk skorunu al ve ağırlığıyla çarp
        for (FraudRule rule : fraudRules) {
            double riskScore = rule.calculateRisk(transaction);
            String ruleName = rule.getRuleName();
            BigDecimal weight = ruleWeights.getOrDefault(ruleName, BigDecimal.ZERO);
            
            BigDecimal weightedScore = BigDecimal.valueOf(riskScore)
                    .multiply(weight)
                    .setScale(RISK_SCORE_SCALE, RoundingMode.HALF_UP);
            
            totalWeightedScore = totalWeightedScore.add(weightedScore);
            
            logger.debug("Kural: {} - Risk: {} - Ağırlık: {} - Ağırlıklı Skor: {}", 
                       ruleName, riskScore, weight, weightedScore);
        }
        
        // Risk skorunu 0.0 - 1.0 aralığına normalize et
        BigDecimal normalizedScore = totalWeightedScore
                .max(MIN_RISK_SCORE)
                .min(MAX_RISK_SCORE)
                .setScale(RISK_SCORE_SCALE, RoundingMode.HALF_UP);
        
        logger.debug("İşlem {} için nihai risk skoru: {}", transaction.getTransactionId(), normalizedScore);
        
        return normalizedScore;
    }
    
    /**
     * Tüm kurallardan risk açıklamalarını toplar.
     * 
     * @param transaction Analiz edilen işlem
     * @return Risk açıklamalarının listesi
     */
    public List<String> getRiskReasons(Transaction transaction) {
        return fraudRules.stream()
                .map(rule -> rule.getRiskReason(transaction))
                .filter(reason -> reason != null && !reason.isEmpty())
                .collect(Collectors.toList());
    }
}



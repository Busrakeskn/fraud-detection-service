package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * İşlem sıklığı bazlı dolandırıcılık tespit kuralı.
 * 
 * Aynı karttan kısa bir süre içinde yapılan çoklu işlemler
 * dolandırıcılık riskini artırır. Bu kural, belirli bir zaman
 * penceresi içindeki işlem sayısını takip eder.
 * 
 * NOT: Şu an in-memory çözüm kullanılmaktadır. Üretim ortamında
 * bu yapı Redis veya Hazelcast gibi dağıtılmış önbellek sistemleri
 * ile değiştirilmelidir. Dağıtılmış önbellek kullanımı:
 * - Çoklu instance'larda tutarlı sonuçlar sağlar
 * - Yüksek trafikli ortamlarda performansı artırır
 * - Veri kaybını önler (instance restart durumlarında)
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@Component
public class FrequencyRule implements FraudRule {
    
    private static final Logger logger = LoggerFactory.getLogger(FrequencyRule.class);
    
    // Risk skorları
    private static final double HIGH_FREQUENCY_RISK = 0.85;
    private static final double MODERATE_FREQUENCY_RISK = 0.4;
    private static final double NORMAL_FREQUENCY_RISK = 0.0;
    
    // Eşik değerleri
    private static final int MODERATE_FREQUENCY_THRESHOLD = 2;
    
    // Yapılandırma değerleri
    private final int timeWindowMinutes;
    private final int suspiciousTransactionCount;
    
    // İşlem geçmişi - in-memory (üretimde Redis/Hazelcast kullanılmalı)
    // TODO: Üretimde Redis veya Hazelcast ile değiştir:
    // @Autowired
    // private RedisTemplate<String, List<LocalDateTime>> redisTemplate;
    private final ConcurrentMap<String, List<LocalDateTime>> cardTransactionHistory;
    
    /**
     * Constructor - yapılandırma değerlerini enjekte eder.
     * 
     * @param timeWindowMinutes Sıklık kontrolü için zaman penceresi (dakika) - varsayılan: 15
     * @param suspiciousTransactionCount Şüpheli kabul edilen işlem sayısı - varsayılan: 3
     */
    public FrequencyRule(
            @Value("${fraud.detection.frequency.time-window-minutes:15}") int timeWindowMinutes,
            @Value("${fraud.detection.frequency.suspicious-count:3}") int suspiciousTransactionCount) {
        this.timeWindowMinutes = timeWindowMinutes;
        this.suspiciousTransactionCount = suspiciousTransactionCount;
        this.cardTransactionHistory = new ConcurrentHashMap<>();
    }
    
    @Override
    public double calculateRisk(Transaction transaction) {
        if (transaction == null || transaction.getCardNumber() == null || 
            transaction.getTransactionTimestamp() == null) {
            logger.warn("Geçersiz işlem - FrequencyRule");
            return NORMAL_FREQUENCY_RISK;
        }
        
        String cardNumber = transaction.getCardNumber();
        LocalDateTime currentTimestamp = transaction.getTransactionTimestamp();
        
        // Son işlemleri al
        List<LocalDateTime> recentTransactions = getRecentTransactions(cardNumber, currentTimestamp);
        
        // İşlemi geçmişe ekle (mevcut işlem dahil değil)
        recordTransaction(cardNumber, currentTimestamp);
        
        // Risk değerlendirmesi
        if (recentTransactions.size() >= suspiciousTransactionCount) {
            logger.debug("Şüpheli sıklık tespit edildi: {} dakika içinde {} işlem - Kart: {}", 
                        timeWindowMinutes, recentTransactions.size(), maskCardNumber(cardNumber));
            return HIGH_FREQUENCY_RISK;
        } else if (recentTransactions.size() >= MODERATE_FREQUENCY_THRESHOLD) {
            logger.debug("Orta sıklık tespit edildi: {} dakika içinde {} işlem - Kart: {}", 
                        timeWindowMinutes, recentTransactions.size(), maskCardNumber(cardNumber));
            return MODERATE_FREQUENCY_RISK;
        }
        
        return NORMAL_FREQUENCY_RISK;
    }
    
    /**
     * Zaman penceresi içindeki son işlemleri alır.
     * 
     * @param cardNumber Kart numarası
     * @param currentTimestamp Mevcut işlem zaman damgası
     * @return Son işlem zaman damgalarının listesi
     */
    private List<LocalDateTime> getRecentTransactions(String cardNumber, LocalDateTime currentTimestamp) {
        List<LocalDateTime> allTransactions = cardTransactionHistory.getOrDefault(cardNumber, new ArrayList<>());
        LocalDateTime windowStart = currentTimestamp.minusMinutes(timeWindowMinutes);
        
        return allTransactions.stream()
                .filter(timestamp -> timestamp.isAfter(windowStart) && timestamp.isBefore(currentTimestamp))
                .toList();
    }
    
    /**
     * İşlemi geçmişe kaydeder.
     * 
     * @param cardNumber Kart numarası
     * @param timestamp İşlem zaman damgası
     */
    private void recordTransaction(String cardNumber, LocalDateTime timestamp) {
        cardTransactionHistory.computeIfAbsent(cardNumber, k -> new ArrayList<>()).add(timestamp);
        
        // Bellek sızıntısını önlemek için eski işlemleri temizle (24 saatten eski)
        cleanupOldTransactions(cardNumber, timestamp);
    }
    
    /**
     * 24 saatten eski işlemleri temizler.
     * 
     * @param cardNumber Kart numarası
     * @param currentTimestamp Mevcut zaman damgası
     */
    private void cleanupOldTransactions(String cardNumber, LocalDateTime currentTimestamp) {
        List<LocalDateTime> transactions = cardTransactionHistory.get(cardNumber);
        if (transactions != null) {
            LocalDateTime cutoff = currentTimestamp.minusHours(24);
            transactions.removeIf(timestamp -> timestamp.isBefore(cutoff));
        }
    }
    
    /**
     * Kart numarasını güvenlik amacıyla maskeler.
     * 
     * @param cardNumber Kart numarası
     * @return Maskelenmiş kart numarası
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
    
    @Override
    public String getRuleName() {
        return "Sıklık Kuralı";
    }
    
    @Override
    public String getRiskReason(Transaction transaction) {
        if (transaction == null || transaction.getCardNumber() == null || 
            transaction.getTransactionTimestamp() == null) {
            return null;
        }
        
        String cardNumber = transaction.getCardNumber();
        LocalDateTime currentTimestamp = transaction.getTransactionTimestamp();
        List<LocalDateTime> recentTransactions = getRecentTransactions(cardNumber, currentTimestamp);
        
        double risk = calculateRisk(transaction);
        if (risk > 0) {
            return String.format("Kısa süre içinde çoklu işlem tespit edildi (%d dakika içinde %d işlem, eşik: %d)", 
                               timeWindowMinutes, recentTransactions.size() + 1, suspiciousTransactionCount);
        }
        
        return null;
    }
}



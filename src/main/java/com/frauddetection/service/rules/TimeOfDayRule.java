package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Gün saati bazlı dolandırıcılık tespit kuralı.
 * 
 * Gece saatlerinde (00:00-05:00) yapılan işlemler şüpheli kabul edilir
 * ve yüksek risk skoru üretir. Bu kural, bankacılık sektöründe yaygın
 * olarak kullanılan bir dolandırıcılık tespit desenidir.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@Component
public class TimeOfDayRule implements FraudRule {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeOfDayRule.class);
    
    // Şüpheli saat aralığı - config'den okunabilir
    private final int suspiciousStartHour;
    private final int suspiciousEndHour;
    
    // Gece işlemleri için risk skoru
    private static final double NIGHT_TRANSACTION_RISK = 0.8;
    private static final double NORMAL_HOURS_RISK = 0.0;
    
    /**
     * Constructor - yapılandırma değerlerini enjekte eder.
     * 
     * @param suspiciousStartHour Şüpheli saat aralığının başlangıcı (varsayılan: 0)
     * @param suspiciousEndHour Şüpheli saat aralığının sonu (varsayılan: 5)
     */
    public TimeOfDayRule(
            @Value("${fraud.detection.rules.time-of-day.start-hour:0}") int suspiciousStartHour,
            @Value("${fraud.detection.rules.time-of-day.end-hour:5}") int suspiciousEndHour) {
        this.suspiciousStartHour = suspiciousStartHour;
        this.suspiciousEndHour = suspiciousEndHour;
    }
    
    @Override
    public double calculateRisk(Transaction transaction) {
        if (transaction == null || transaction.getTransactionTimestamp() == null) {
            logger.warn("Geçersiz işlem veya zaman damgası - TimeOfDayRule");
            return NORMAL_HOURS_RISK;
        }
        
        LocalTime transactionTime = transaction.getTransactionTimestamp().toLocalTime();
        LocalTime startTime = LocalTime.of(suspiciousStartHour, 0);
        LocalTime endTime = LocalTime.of(suspiciousEndHour, 0);
        
        // Şüpheli saat aralığında mı kontrol et
        boolean isSuspiciousTime = isTimeInRange(transactionTime, startTime, endTime);
        
        if (isSuspiciousTime) {
            logger.debug("Şüpheli saatlerde işlem tespit edildi: {} - İşlem ID: {}", 
                        transactionTime, transaction.getTransactionId());
            return NIGHT_TRANSACTION_RISK;
        }
        
        return NORMAL_HOURS_RISK;
    }
    
    /**
     * Zamanın belirtilen aralıkta olup olmadığını kontrol eder.
     * Gece yarısını geçen durumları da dikkate alır (örn: 00:00-05:00).
     * 
     * @param time Kontrol edilecek zaman
     * @param start Başlangıç zamanı
     * @param end Bitiş zamanı
     * @return Zaman aralık içindeyse true
     */
    private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
        // Gece yarısını geçen aralık kontrolü
        if (start.isAfter(end) || start.equals(end)) {
            // Gece yarısını geçen durum (örn: 22:00-02:00)
            return time.isAfter(start) || time.isBefore(end);
        } else {
            // Normal aralık (örn: 00:00-05:00)
            return time.isAfter(start) && time.isBefore(end);
        }
    }
    
    @Override
    public String getRuleName() {
        return "Gün Saati Kuralı";
    }
    
    @Override
    public String getRiskReason(Transaction transaction) {
        if (transaction == null || transaction.getTransactionTimestamp() == null) {
            return null;
        }
        
        double risk = calculateRisk(transaction);
        if (risk > 0) {
            LocalTime time = transaction.getTransactionTimestamp().toLocalTime();
            return String.format("İşlem şüpheli saatlerde gerçekleşti (%02d:00-%02d:00 arası, gerçekleşme: %s)",
                               suspiciousStartHour, suspiciousEndHour, time);
        }
        
        return null;
    }
}



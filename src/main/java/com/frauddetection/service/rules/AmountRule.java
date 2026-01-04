package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * İşlem tutarı bazlı dolandırıcılık tespit kuralı.
 * 
 * Yüksek tutarlı işlemler dolandırıcılık riskini artırır. Bu kural,
 * tutarı üç seviyede değerlendirir:
 * - Düşük tutarlar: Minimal risk
 * - Yüksek tutarlar: Orta-yüksek risk (doğrusal ölçekleme)
 * - Çok yüksek tutarlar: Maksimum risk
 * 
 * Eşik değerleri application.yml'den yapılandırılabilir.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@Component
public class AmountRule implements FraudRule {
    
    private static final Logger logger = LoggerFactory.getLogger(AmountRule.class);
    
    // Risk skorları - magic number kullanmamak için sabitler
    private static final double VERY_HIGH_AMOUNT_RISK = 0.9;
    private static final double HIGH_AMOUNT_BASE_RISK = 0.5;
    private static final double HIGH_AMOUNT_MAX_RISK = 0.9;
    private static final double LOW_AMOUNT_MAX_RISK = 0.3;
    
    // Tutar eşikleri - config'den okunur
    private final BigDecimal highAmountThreshold;
    private final BigDecimal veryHighAmountThreshold;
    
    /**
     * Constructor - yapılandırma değerlerini enjekte eder.
     * 
     * @param highAmountThreshold Yüksek tutar eşiği (varsayılan: 10000.0)
     * @param veryHighAmountThreshold Çok yüksek tutar eşiği (varsayılan: 50000.0)
     */
    public AmountRule(
            @Value("${fraud.detection.amount.high-threshold:10000.0}") BigDecimal highAmountThreshold,
            @Value("${fraud.detection.amount.very-high-threshold:50000.0}") BigDecimal veryHighAmountThreshold) {
        this.highAmountThreshold = highAmountThreshold;
        this.veryHighAmountThreshold = veryHighAmountThreshold;
    }
    
    @Override
    public double calculateRisk(Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            logger.warn("Geçersiz işlem veya tutar - AmountRule");
            return 0.0;
        }
        
        BigDecimal amount = transaction.getAmount();
        
        // Çok yüksek tutar kontrolü
        if (amount.compareTo(veryHighAmountThreshold) >= 0) {
            logger.debug("Çok yüksek tutarlı işlem tespit edildi: {} - İşlem ID: {}", 
                        amount, transaction.getTransactionId());
            return VERY_HIGH_AMOUNT_RISK;
        }
        
        // Yüksek tutar kontrolü (doğrusal ölçekleme)
        if (amount.compareTo(highAmountThreshold) >= 0) {
            logger.debug("Yüksek tutarlı işlem tespit edildi: {} - İşlem ID: {}", 
                        amount, transaction.getTransactionId());
            
            // Yüksek ve çok yüksek eşik arasında doğrusal ölçekleme
            BigDecimal thresholdDiff = veryHighAmountThreshold.subtract(highAmountThreshold);
            BigDecimal amountDiff = amount.subtract(highAmountThreshold);
            
            if (thresholdDiff.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = amountDiff.divide(thresholdDiff, 4, RoundingMode.HALF_UP);
                double additionalRisk = ratio.multiply(BigDecimal.valueOf(HIGH_AMOUNT_MAX_RISK - HIGH_AMOUNT_BASE_RISK))
                                             .doubleValue();
                return HIGH_AMOUNT_BASE_RISK + additionalRisk;
            }
            
            return HIGH_AMOUNT_BASE_RISK;
        }
        
        // Düşük-orta tutarlar için minimal risk (tutara göre ölçeklenmiş)
        BigDecimal ratio = amount.divide(highAmountThreshold, 4, RoundingMode.HALF_UP);
        double risk = ratio.multiply(BigDecimal.valueOf(LOW_AMOUNT_MAX_RISK)).doubleValue();
        return Math.min(risk, LOW_AMOUNT_MAX_RISK);
    }
    
    @Override
    public String getRuleName() {
        return "Tutar Kuralı";
    }
    
    @Override
    public String getRiskReason(Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return null;
        }
        
        BigDecimal amount = transaction.getAmount();
        double risk = calculateRisk(transaction);
        
        if (risk > 0) {
            if (amount.compareTo(veryHighAmountThreshold) >= 0) {
                return String.format("Çok yüksek işlem tutarı tespit edildi (%.2f, eşik: %.2f)", 
                                    amount.doubleValue(), veryHighAmountThreshold.doubleValue());
            } else if (amount.compareTo(highAmountThreshold) >= 0) {
                return String.format("Yüksek işlem tutarı tespit edildi (%.2f, eşik: %.2f)", 
                                    amount.doubleValue(), highAmountThreshold.doubleValue());
            }
        }
        
        return null;
    }
}



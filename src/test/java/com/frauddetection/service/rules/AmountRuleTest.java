package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AmountRule için unit testler.
 * 
 * Given/When/Then yapısı kullanılarak test senaryoları yazılmıştır.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@DisplayName("AmountRule Unit Tests")
class AmountRuleTest {
    
    private AmountRule amountRule;
    
    @BeforeEach
    void setUp() {
        // Given: AmountRule instance'ı oluştur (varsayılan eşiklerle)
        amountRule = new AmountRule(
                new BigDecimal("10000.0"),  // high-threshold
                new BigDecimal("50000.0")  // very-high-threshold
        );
    }
    
    @Test
    @DisplayName("Düşük tutarlı işlem için düşük risk döndürmeli")
    void calculateRisk_LowAmount_ShouldReturnLowRisk() {
        // Given: Düşük tutarlı bir işlem (eşiğin altında)
        Transaction transaction = createTransaction("TXN-001", "4111111111111111", 
                                                    new BigDecimal("5000.0"), 
                                                    LocalDateTime.now());
        
        // When: Risk skoru hesaplanır
        double riskScore = amountRule.calculateRisk(transaction);
        
        // Then: Risk skoru düşük olmalı (0.0 - 0.3 arası)
        assertTrue(riskScore >= 0.0 && riskScore <= 0.3, 
                  "Düşük tutarlı işlem için risk skoru 0.0-0.3 arasında olmalı");
    }
    
    @Test
    @DisplayName("Yüksek tutarlı işlem için yüksek risk döndürmeli")
    void calculateRisk_HighAmount_ShouldReturnHighRisk() {
        // Given: Yüksek tutarlı bir işlem (high-threshold üzerinde, very-high-threshold altında)
        Transaction transaction = createTransaction("TXN-002", "4111111111111111", 
                                                    new BigDecimal("30000.0"), 
                                                    LocalDateTime.now());
        
        // When: Risk skoru hesaplanır
        double riskScore = amountRule.calculateRisk(transaction);
        
        // Then: Risk skoru yüksek olmalı (0.5 - 0.9 arası)
        assertTrue(riskScore >= 0.5 && riskScore <= 0.9, 
                  "Yüksek tutarlı işlem için risk skoru 0.5-0.9 arasında olmalı");
    }
    
    @Test
    @DisplayName("Çok yüksek tutarlı işlem için maksimum risk döndürmeli")
    void calculateRisk_VeryHighAmount_ShouldReturnMaximumRisk() {
        // Given: Çok yüksek tutarlı bir işlem (very-high-threshold üzerinde)
        Transaction transaction = createTransaction("TXN-003", "4111111111111111", 
                                                    new BigDecimal("60000.0"), 
                                                    LocalDateTime.now());
        
        // When: Risk skoru hesaplanır
        double riskScore = amountRule.calculateRisk(transaction);
        
        // Then: Risk skoru maksimum olmalı (0.9)
        assertEquals(0.9, riskScore, 0.01, 
                    "Çok yüksek tutarlı işlem için risk skoru 0.9 olmalı");
    }
    
    @Test
    @DisplayName("Null işlem için sıfır risk döndürmeli")
    void calculateRisk_NullTransaction_ShouldReturnZero() {
        // Given: Null işlem
        Transaction transaction = null;
        
        // When: Risk skoru hesaplanır
        double riskScore = amountRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Null işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Null tutarlı işlem için sıfır risk döndürmeli")
    void calculateRisk_NullAmount_ShouldReturnZero() {
        // Given: Tutarı null olan bir işlem
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-004");
        transaction.setAmount(null);
        
        // When: Risk skoru hesaplanır
        double riskScore = amountRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Null tutarlı işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Kural adı doğru döndürülmeli")
    void getRuleName_ShouldReturnCorrectName() {
        // Given: AmountRule instance'ı
        
        // When: Kural adı alınır
        String ruleName = amountRule.getRuleName();
        
        // Then: Kural adı "Tutar Kuralı" olmalı
        assertEquals("Tutar Kuralı", ruleName, "Kural adı 'Tutar Kuralı' olmalı");
    }
    
    @Test
    @DisplayName("Yüksek riskli işlem için risk sebebi döndürmeli")
    void getRiskReason_HighAmount_ShouldReturnReason() {
        // Given: Yüksek tutarlı bir işlem
        Transaction transaction = createTransaction("TXN-005", "4111111111111111", 
                                                    new BigDecimal("30000.0"), 
                                                    LocalDateTime.now());
        
        // When: Risk sebebi alınır
        String reason = amountRule.getRiskReason(transaction);
        
        // Then: Risk sebebi null olmamalı ve tutar bilgisi içermeli
        assertNotNull(reason, "Yüksek tutarlı işlem için risk sebebi döndürülmeli");
        assertTrue(reason.contains("Yüksek işlem tutarı"), 
                  "Risk sebebi 'Yüksek işlem tutarı' içermeli");
    }
    
    @Test
    @DisplayName("Düşük riskli işlem için risk sebebi null döndürmeli")
    void getRiskReason_LowAmount_ShouldReturnNull() {
        // Given: Düşük tutarlı bir işlem
        Transaction transaction = createTransaction("TXN-006", "4111111111111111", 
                                                    new BigDecimal("1000.0"), 
                                                    LocalDateTime.now());
        
        // When: Risk sebebi alınır
        String reason = amountRule.getRiskReason(transaction);
        
        // Then: Risk sebebi null olmalı
        assertNull(reason, "Düşük tutarlı işlem için risk sebebi null olmalı");
    }
    
    // Helper method
    private Transaction createTransaction(String transactionId, String cardNumber, 
                                        BigDecimal amount, LocalDateTime timestamp) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setCardNumber(cardNumber);
        transaction.setAmount(amount);
        transaction.setTransactionTimestamp(timestamp);
        return transaction;
    }
}



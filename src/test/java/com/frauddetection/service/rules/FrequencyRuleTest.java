package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FrequencyRule için unit testler.
 * 
 * Given/When/Then yapısı kullanılarak test senaryoları yazılmıştır.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@DisplayName("FrequencyRule Unit Tests")
class FrequencyRuleTest {
    
    private FrequencyRule frequencyRule;
    
    @BeforeEach
    void setUp() {
        // Given: FrequencyRule instance'ı oluştur (15 dakika penceresi, 3 şüpheli işlem)
        frequencyRule = new FrequencyRule(15, 3);
    }
    
    @Test
    @DisplayName("İlk işlem için düşük risk döndürmeli")
    void calculateRisk_FirstTransaction_ShouldReturnLowRisk() {
        // Given: İlk işlem (geçmişte başka işlem yok)
        LocalDateTime now = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        Transaction transaction = createTransaction("TXN-001", "4111111111111111", now);
        
        // When: Risk skoru hesaplanır
        double riskScore = frequencyRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "İlk işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Kısa sürede çoklu işlem için yüksek risk döndürmeli")
    void calculateRisk_MultipleTransactionsInShortTime_ShouldReturnHighRisk() {
        // Given: Aynı karttan kısa sürede yapılan 3 işlem
        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        
        // İlk iki işlem (geçmişte)
        Transaction transaction1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(5));
        Transaction transaction2 = createTransaction("TXN-002", cardNumber, baseTime.minusMinutes(3));
        
        // İlk iki işlemi kaydet (kural içinde kaydedilecek)
        frequencyRule.calculateRisk(transaction1);
        frequencyRule.calculateRisk(transaction2);
        
        // Üçüncü işlem (şu an)
        Transaction transaction3 = createTransaction("TXN-003", cardNumber, baseTime);
        
        // When: Üçüncü işlem için risk skoru hesaplanır
        double riskScore = frequencyRule.calculateRisk(transaction3);
        
        // Then: Risk skoru yüksek olmalı (0.85)
        assertEquals(0.85, riskScore, 0.01, 
                    "Kısa sürede çoklu işlem için risk skoru 0.85 olmalı");
    }
    
    @Test
    @DisplayName("İki işlem için orta risk döndürmeli")
    void calculateRisk_TwoTransactions_ShouldReturnModerateRisk() {
        // Given: Aynı karttan yapılan 2 işlem
        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        
        // İlk işlem
        Transaction transaction1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(5));
        frequencyRule.calculateRisk(transaction1);
        
        // İkinci işlem
        Transaction transaction2 = createTransaction("TXN-002", cardNumber, baseTime);
        
        // When: İkinci işlem için risk skoru hesaplanır
        double riskScore = frequencyRule.calculateRisk(transaction2);
        
        // Then: Risk skoru orta olmalı (0.4)
        assertEquals(0.4, riskScore, 0.01, 
                    "İki işlem için risk skoru 0.4 olmalı");
    }
    
    @Test
    @DisplayName("Zaman penceresi dışındaki işlem için düşük risk döndürmeli")
    void calculateRisk_TransactionOutsideTimeWindow_ShouldReturnLowRisk() {
        // Given: Zaman penceresi dışında (20 dakika önce) yapılan işlem
        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        
        // İlk işlem (20 dakika önce - zaman penceresi dışında)
        Transaction transaction1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(20));
        frequencyRule.calculateRisk(transaction1);
        
        // İkinci işlem (şu an)
        Transaction transaction2 = createTransaction("TXN-002", cardNumber, baseTime);
        
        // When: İkinci işlem için risk skoru hesaplanır
        double riskScore = frequencyRule.calculateRisk(transaction2);
        
        // Then: Risk skoru 0.0 olmalı (zaman penceresi dışında)
        assertEquals(0.0, riskScore, 
                    "Zaman penceresi dışındaki işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Null işlem için sıfır risk döndürmeli")
    void calculateRisk_NullTransaction_ShouldReturnZero() {
        // Given: Null işlem
        Transaction transaction = null;
        
        // When: Risk skoru hesaplanır
        double riskScore = frequencyRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Null işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Null kart numarası için sıfır risk döndürmeli")
    void calculateRisk_NullCardNumber_ShouldReturnZero() {
        // Given: Kart numarası null olan bir işlem
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-001");
        transaction.setCardNumber(null);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        
        // When: Risk skoru hesaplanır
        double riskScore = frequencyRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Null kart numarası için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Kural adı doğru döndürülmeli")
    void getRuleName_ShouldReturnCorrectName() {
        // Given: FrequencyRule instance'ı
        
        // When: Kural adı alınır
        String ruleName = frequencyRule.getRuleName();
        
        // Then: Kural adı "Sıklık Kuralı" olmalı
        assertEquals("Sıklık Kuralı", ruleName, "Kural adı 'Sıklık Kuralı' olmalı");
    }
    
    @Test
    @DisplayName("Yüksek sıklık için risk sebebi döndürmeli")
    void getRiskReason_HighFrequency_ShouldReturnReason() {
        // Given: Kısa sürede çoklu işlem
        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        
        Transaction transaction1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(5));
        Transaction transaction2 = createTransaction("TXN-002", cardNumber, baseTime.minusMinutes(3));
        frequencyRule.calculateRisk(transaction1);
        frequencyRule.calculateRisk(transaction2);
        
        Transaction transaction3 = createTransaction("TXN-003", cardNumber, baseTime);
        
        // When: Risk sebebi alınır
        String reason = frequencyRule.getRiskReason(transaction3);
        
        // Then: Risk sebebi null olmamalı ve sıklık bilgisi içermeli
        assertNotNull(reason, "Yüksek sıklık için risk sebebi döndürülmeli");
        assertTrue(reason.contains("çoklu işlem"), 
                  "Risk sebebi 'çoklu işlem' içermeli");
    }
    
    // Helper method
    private Transaction createTransaction(String transactionId, String cardNumber, 
                                        LocalDateTime timestamp) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setCardNumber(cardNumber);
        transaction.setTransactionTimestamp(timestamp);
        return transaction;
    }
}



package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeOfDayRule için unit testler.
 * 
 * Given/When/Then yapısı kullanılarak test senaryoları yazılmıştır.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@DisplayName("TimeOfDayRule Unit Tests")
class TimeOfDayRuleTest {
    
    private TimeOfDayRule timeOfDayRule;
    
    @BeforeEach
    void setUp() {
        // Given: TimeOfDayRule instance'ı oluştur (varsayılan saat aralığı: 0-5)
        timeOfDayRule = new TimeOfDayRule(0, 5);
    }
    
    @Test
    @DisplayName("Normal saatlerde yapılan işlem için düşük risk döndürmeli")
    void calculateRisk_NormalHours_ShouldReturnLowRisk() {
        // Given: Normal saatlerde (10:00) yapılan bir işlem
        LocalDateTime normalTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        Transaction transaction = createTransaction("TXN-001", "4111111111111111", normalTime);
        
        // When: Risk skoru hesaplanır
        double riskScore = timeOfDayRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Normal saatlerde yapılan işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Şüpheli saatlerde yapılan işlem için yüksek risk döndürmeli")
    void calculateRisk_SuspiciousHours_ShouldReturnHighRisk() {
        // Given: Şüpheli saatlerde (03:00) yapılan bir işlem
        LocalDateTime suspiciousTime = LocalDateTime.of(2024, 12, 27, 3, 0, 0);
        Transaction transaction = createTransaction("TXN-002", "4111111111111111", suspiciousTime);
        
        // When: Risk skoru hesaplanır
        double riskScore = timeOfDayRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.8 olmalı
        assertEquals(0.8, riskScore, 0.01, 
                    "Şüpheli saatlerde yapılan işlem için risk skoru 0.8 olmalı");
    }
    
    @Test
    @DisplayName("Gece yarısında yapılan işlem için yüksek risk döndürmeli")
    void calculateRisk_Midnight_ShouldReturnHighRisk() {
        // Given: Gece yarısında (00:30) yapılan bir işlem
        LocalDateTime midnightTime = LocalDateTime.of(2024, 12, 27, 0, 30, 0);
        Transaction transaction = createTransaction("TXN-003", "4111111111111111", midnightTime);
        
        // When: Risk skoru hesaplanır
        double riskScore = timeOfDayRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.8 olmalı
        assertEquals(0.8, riskScore, 0.01, 
                    "Gece yarısında yapılan işlem için risk skoru 0.8 olmalı");
    }
    
    @Test
    @DisplayName("Şüpheli saat aralığının sonunda yapılan işlem için yüksek risk döndürmeli")
    void calculateRisk_EndOfSuspiciousHours_ShouldReturnHighRisk() {
        // Given: Şüpheli saat aralığının sonunda (04:59) yapılan bir işlem
        LocalDateTime endTime = LocalDateTime.of(2024, 12, 27, 4, 59, 0);
        Transaction transaction = createTransaction("TXN-004", "4111111111111111", endTime);
        
        // When: Risk skoru hesaplanır
        double riskScore = timeOfDayRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.8 olmalı
        assertEquals(0.8, riskScore, 0.01, 
                    "Şüpheli saat aralığının sonunda yapılan işlem için risk skoru 0.8 olmalı");
    }
    
    @Test
    @DisplayName("Null işlem için sıfır risk döndürmeli")
    void calculateRisk_NullTransaction_ShouldReturnZero() {
        // Given: Null işlem
        Transaction transaction = null;
        
        // When: Risk skoru hesaplanır
        double riskScore = timeOfDayRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Null işlem için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Null zaman damgası için sıfır risk döndürmeli")
    void calculateRisk_NullTimestamp_ShouldReturnZero() {
        // Given: Zaman damgası null olan bir işlem
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-005");
        transaction.setTransactionTimestamp(null);
        
        // When: Risk skoru hesaplanır
        double riskScore = timeOfDayRule.calculateRisk(transaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore, "Null zaman damgası için risk skoru 0.0 olmalı");
    }
    
    @Test
    @DisplayName("Kural adı doğru döndürülmeli")
    void getRuleName_ShouldReturnCorrectName() {
        // Given: TimeOfDayRule instance'ı
        
        // When: Kural adı alınır
        String ruleName = timeOfDayRule.getRuleName();
        
        // Then: Kural adı "Gün Saati Kuralı" olmalı
        assertEquals("Gün Saati Kuralı", ruleName, "Kural adı 'Gün Saati Kuralı' olmalı");
    }
    
    @Test
    @DisplayName("Şüpheli saatlerde yapılan işlem için risk sebebi döndürmeli")
    void getRiskReason_SuspiciousHours_ShouldReturnReason() {
        // Given: Şüpheli saatlerde (02:00) yapılan bir işlem
        LocalDateTime suspiciousTime = LocalDateTime.of(2024, 12, 27, 2, 0, 0);
        Transaction transaction = createTransaction("TXN-006", "4111111111111111", suspiciousTime);
        
        // When: Risk sebebi alınır
        String reason = timeOfDayRule.getRiskReason(transaction);
        
        // Then: Risk sebebi null olmamalı ve saat bilgisi içermeli
        assertNotNull(reason, "Şüpheli saatlerde yapılan işlem için risk sebebi döndürülmeli");
        assertTrue(reason.contains("şüpheli saatlerde"), 
                  "Risk sebebi 'şüpheli saatlerde' içermeli");
    }
    
    @Test
    @DisplayName("Normal saatlerde yapılan işlem için risk sebebi null döndürmeli")
    void getRiskReason_NormalHours_ShouldReturnNull() {
        // Given: Normal saatlerde (14:00) yapılan bir işlem
        LocalDateTime normalTime = LocalDateTime.of(2024, 12, 27, 14, 0, 0);
        Transaction transaction = createTransaction("TXN-007", "4111111111111111", normalTime);
        
        // When: Risk sebebi alınır
        String reason = timeOfDayRule.getRiskReason(transaction);
        
        // Then: Risk sebebi null olmalı
        assertNull(reason, "Normal saatlerde yapılan işlem için risk sebebi null olmalı");
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



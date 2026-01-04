package com.frauddetection.service;

import com.frauddetection.domain.Transaction;
import com.frauddetection.service.rules.FraudRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RiskScoreCalculator için unit testler.
 * 
 * Given/When/Then yapısı kullanılarak test senaryoları yazılmıştır.
 * FraudRule implementasyonları mock'lanmıştır.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreCalculator Unit Tests")
class RiskScoreCalculatorTest {
    
    @Mock
    private FraudRule timeOfDayRule;
    
    @Mock
    private FraudRule amountRule;
    
    @Mock
    private FraudRule frequencyRule;
    
    private RiskScoreCalculator riskScoreCalculator;
    private Transaction testTransaction;
    
    @BeforeEach
    void setUp() {
        // Given: Test işlemi oluştur
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN-001");
        testTransaction.setCardNumber("4111111111111111");
        testTransaction.setAmount(new BigDecimal("15000.0"));
        testTransaction.setTransactionTimestamp(LocalDateTime.now());
        
        // Mock kuralların isimlerini ayarla
        when(timeOfDayRule.getRuleName()).thenReturn("Gün Saati Kuralı");
        when(amountRule.getRuleName()).thenReturn("Tutar Kuralı");
        when(frequencyRule.getRuleName()).thenReturn("Sıklık Kuralı");
        
        // Given: RiskScoreCalculator instance'ı oluştur (mock kurallarla)
        List<FraudRule> fraudRules = Arrays.asList(timeOfDayRule, amountRule, frequencyRule);
        riskScoreCalculator = new RiskScoreCalculator(
                fraudRules,
                new BigDecimal("0.30"),  // time-of-day-weight
                new BigDecimal("0.35"),  // amount-weight
                new BigDecimal("0.35")   // frequency-weight
        );
    }
    
    @Test
    @DisplayName("Tüm kurallardan gelen risk skorları ağırlıklarıyla toplanmalı")
    void calculateFinalRiskScore_ShouldCalculateWeightedSum() {
        // Given: Her kuraldan farklı risk skorları
        when(timeOfDayRule.calculateRisk(testTransaction)).thenReturn(0.8);  // Gece işlemi
        when(amountRule.calculateRisk(testTransaction)).thenReturn(0.6);      // Yüksek tutar
        when(frequencyRule.calculateRisk(testTransaction)).thenReturn(0.0);    // Normal sıklık
        
        // When: Nihai risk skoru hesaplanır
        BigDecimal riskScore = riskScoreCalculator.calculateFinalRiskScore(testTransaction);
        
        // Then: Ağırlıklı toplam doğru hesaplanmalı
        // 0.8 * 0.30 + 0.6 * 0.35 + 0.0 * 0.35 = 0.24 + 0.21 + 0.0 = 0.45
        assertEquals(0.45, riskScore.doubleValue(), 0.01, 
                    "Ağırlıklı risk skoru doğru hesaplanmalı");
        
        // Verify: Her kural çağrıldı
        verify(timeOfDayRule, times(1)).calculateRisk(testTransaction);
        verify(amountRule, times(1)).calculateRisk(testTransaction);
        verify(frequencyRule, times(1)).calculateRisk(testTransaction);
    }
    
    @Test
    @DisplayName("Risk skoru 1.0'ı geçerse 1.0'a normalize edilmeli")
    void calculateFinalRiskScore_ShouldNormalizeToMax() {
        // Given: Tüm kurallar maksimum risk döndürüyor
        when(timeOfDayRule.calculateRisk(testTransaction)).thenReturn(1.0);
        when(amountRule.calculateRisk(testTransaction)).thenReturn(1.0);
        when(frequencyRule.calculateRisk(testTransaction)).thenReturn(1.0);
        
        // When: Nihai risk skoru hesaplanır
        BigDecimal riskScore = riskScoreCalculator.calculateFinalRiskScore(testTransaction);
        
        // Then: Risk skoru 1.0'a normalize edilmeli
        assertEquals(1.0, riskScore.doubleValue(), 0.01, 
                    "Risk skoru 1.0'ı geçerse 1.0'a normalize edilmeli");
    }
    
    @Test
    @DisplayName("Risk skoru negatif olsa bile 0.0'a normalize edilmeli")
    void calculateFinalRiskScore_ShouldNormalizeToMin() {
        // Given: Tüm kurallar 0.0 döndürüyor
        when(timeOfDayRule.calculateRisk(testTransaction)).thenReturn(0.0);
        when(amountRule.calculateRisk(testTransaction)).thenReturn(0.0);
        when(frequencyRule.calculateRisk(testTransaction)).thenReturn(0.0);
        
        // When: Nihai risk skoru hesaplanır
        BigDecimal riskScore = riskScoreCalculator.calculateFinalRiskScore(testTransaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore.doubleValue(), 0.01, 
                    "Risk skoru 0.0'dan küçük olamaz");
    }
    
    @Test
    @DisplayName("Null işlem için 0.0 döndürmeli")
    void calculateFinalRiskScore_NullTransaction_ShouldReturnZero() {
        // Given: Null işlem
        Transaction nullTransaction = null;
        
        // When: Nihai risk skoru hesaplanır
        BigDecimal riskScore = riskScoreCalculator.calculateFinalRiskScore(nullTransaction);
        
        // Then: Risk skoru 0.0 olmalı
        assertEquals(0.0, riskScore.doubleValue(), "Null işlem için risk skoru 0.0 olmalı");
        
        // Verify: Kurallar çağrılmamalı
        verify(timeOfDayRule, never()).calculateRisk(any());
        verify(amountRule, never()).calculateRisk(any());
        verify(frequencyRule, never()).calculateRisk(any());
    }
    
    @Test
    @DisplayName("Tüm kurallardan risk açıklamalarını toplamalı")
    void getRiskReasons_ShouldCollectAllReasons() {
        // Given: Her kuraldan risk açıklaması
        when(timeOfDayRule.getRiskReason(testTransaction)).thenReturn("Gece işlemi");
        when(amountRule.getRiskReason(testTransaction)).thenReturn("Yüksek tutar");
        when(frequencyRule.getRiskReason(testTransaction)).thenReturn(null);
        
        // When: Risk açıklamaları alınır
        List<String> reasons = riskScoreCalculator.getRiskReasons(testTransaction);
        
        // Then: Sadece null olmayan açıklamalar döndürülmeli
        assertEquals(2, reasons.size(), "İki risk açıklaması döndürülmeli");
        assertTrue(reasons.contains("Gece işlemi"), "Gece işlemi açıklaması içermeli");
        assertTrue(reasons.contains("Yüksek tutar"), "Yüksek tutar açıklaması içermeli");
    }
    
    @Test
    @DisplayName("Hiç risk açıklaması yoksa boş liste döndürmeli")
    void getRiskReasons_NoRisks_ShouldReturnEmptyList() {
        // Given: Hiçbir kural risk açıklaması döndürmüyor
        when(timeOfDayRule.getRiskReason(testTransaction)).thenReturn(null);
        when(amountRule.getRiskReason(testTransaction)).thenReturn(null);
        when(frequencyRule.getRiskReason(testTransaction)).thenReturn(null);
        
        // When: Risk açıklamaları alınır
        List<String> reasons = riskScoreCalculator.getRiskReasons(testTransaction);
        
        // Then: Boş liste döndürülmeli
        assertTrue(reasons.isEmpty(), "Risk açıklaması yoksa boş liste döndürülmeli");
    }
}



package com.frauddetection.service;

import com.frauddetection.domain.FraudDecision;
import com.frauddetection.domain.Transaction;
import com.frauddetection.dto.FraudResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FraudDetectionService için unit testler.
 * 
 * Given/When/Then yapısı kullanılarak test senaryoları yazılmıştır.
 * RiskScoreCalculator mock'lanmıştır, MeterRegistry gerçek instance kullanılır.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService Unit Tests")
class FraudDetectionServiceTest {
    
    @Mock
    private RiskScoreCalculator riskScoreCalculator;
    
    private SimpleMeterRegistry meterRegistry;
    private FraudDetectionService fraudDetectionService;
    private Transaction testTransaction;
    
    @BeforeEach
    void setUp() {
        // Given: Gerçek MeterRegistry instance'ı (test için yeterli)
        meterRegistry = new SimpleMeterRegistry();
        
        // Given: FraudDetectionService instance'ı oluştur
        fraudDetectionService = new FraudDetectionService(
                riskScoreCalculator,
                meterRegistry,
                new BigDecimal("0.4"),  // approve-max
                new BigDecimal("0.4"),  // review-min
                new BigDecimal("0.7"),  // review-max
                new BigDecimal("0.7")   // block-min
        );
        
        // Given: Test işlemi oluştur
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN-001");
        testTransaction.setCardNumber("4111111111111111");
        testTransaction.setAmount(new BigDecimal("5000.0"));
        testTransaction.setTransactionTimestamp(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("Düşük risk skoru için APPROVE kararı döndürmeli")
    void analyzeTransaction_LowRisk_ShouldReturnApprove() {
        // Given: Düşük risk skoru (0.3 < 0.4)
        BigDecimal lowRiskScore = new BigDecimal("0.3");
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(lowRiskScore);
        when(riskScoreCalculator.getRiskReasons(testTransaction)).thenReturn(Arrays.asList());
        
        // When: İşlem analiz edilir
        FraudResponse response = fraudDetectionService.analyzeTransaction(testTransaction);
        
        // Then: Karar APPROVE olmalı
        assertEquals(FraudDecision.APPROVE, response.getDecision(), 
                    "Düşük risk skoru için karar APPROVE olmalı");
        assertEquals(lowRiskScore, response.getRiskScore(), 
                    "Risk skoru doğru döndürülmeli");
        
        // Verify: RiskScoreCalculator çağrıldı
        verify(riskScoreCalculator, times(1)).calculateFinalRiskScore(testTransaction);
        verify(riskScoreCalculator, times(1)).getRiskReasons(testTransaction);
    }
    
    @Test
    @DisplayName("Orta risk skoru için REVIEW kararı döndürmeli")
    void analyzeTransaction_ModerateRisk_ShouldReturnReview() {
        // Given: Orta risk skoru (0.5, 0.4-0.7 arası)
        BigDecimal moderateRiskScore = new BigDecimal("0.5");
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(moderateRiskScore);
        when(riskScoreCalculator.getRiskReasons(testTransaction))
                .thenReturn(Arrays.asList("Yüksek tutar tespit edildi"));
        
        // When: İşlem analiz edilir
        FraudResponse response = fraudDetectionService.analyzeTransaction(testTransaction);
        
        // Then: Karar REVIEW olmalı
        assertEquals(FraudDecision.REVIEW, response.getDecision(), 
                    "Orta risk skoru için karar REVIEW olmalı");
        assertEquals(moderateRiskScore, response.getRiskScore(), 
                    "Risk skoru doğru döndürülmeli");
        assertNotNull(response.getReason(), "Risk sebebi döndürülmeli");
        
        // Verify: RiskScoreCalculator çağrıldı
        verify(riskScoreCalculator, times(1)).calculateFinalRiskScore(testTransaction);
        verify(riskScoreCalculator, times(1)).getRiskReasons(testTransaction);
    }
    
    @Test
    @DisplayName("Yüksek risk skoru için BLOCK kararı döndürmeli")
    void analyzeTransaction_HighRisk_ShouldReturnBlock() {
        // Given: Yüksek risk skoru (0.8 > 0.7)
        BigDecimal highRiskScore = new BigDecimal("0.8");
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(highRiskScore);
        when(riskScoreCalculator.getRiskReasons(testTransaction))
                .thenReturn(Arrays.asList("Gece işlemi", "Yüksek tutar", "Çoklu işlem"));
        
        // When: İşlem analiz edilir
        FraudResponse response = fraudDetectionService.analyzeTransaction(testTransaction);
        
        // Then: Karar BLOCK olmalı
        assertEquals(FraudDecision.BLOCK, response.getDecision(), 
                    "Yüksek risk skoru için karar BLOCK olmalı");
        assertEquals(highRiskScore, response.getRiskScore(), 
                    "Risk skoru doğru döndürülmeli");
        assertNotNull(response.getReason(), "Risk sebebi döndürülmeli");
        assertTrue(response.getReason().contains(";"), 
                  "Birden fazla risk sebebi birleştirilmeli");
        
        // Verify: RiskScoreCalculator çağrıldı
        verify(riskScoreCalculator, times(1)).calculateFinalRiskScore(testTransaction);
        verify(riskScoreCalculator, times(1)).getRiskReasons(testTransaction);
    }
    
    @Test
    @DisplayName("Eşik değerinde risk skoru için doğru karar döndürmeli")
    void analyzeTransaction_ThresholdValues_ShouldReturnCorrectDecision() {
        // Given: Eşik değerinde risk skorları
        BigDecimal approveThreshold = new BigDecimal("0.4");
        BigDecimal reviewThreshold = new BigDecimal("0.7");
        BigDecimal blockThreshold = new BigDecimal("0.7");
        
        when(riskScoreCalculator.getRiskReasons(testTransaction)).thenReturn(Arrays.asList());
        
        // Test 1: approve-max eşiği (0.4) - REVIEW olmalı (>= review-min)
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(approveThreshold);
        FraudResponse response1 = fraudDetectionService.analyzeTransaction(testTransaction);
        assertEquals(FraudDecision.REVIEW, response1.getDecision(), 
                    "0.4 risk skoru için karar REVIEW olmalı");
        
        // Test 2: block-min eşiği (0.7) - BLOCK olmalı (>= block-min)
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(blockThreshold);
        FraudResponse response2 = fraudDetectionService.analyzeTransaction(testTransaction);
        assertEquals(FraudDecision.BLOCK, response2.getDecision(), 
                    "0.7 risk skoru için karar BLOCK olmalı");
    }
    
    @Test
    @DisplayName("Risk sebebi yoksa varsayılan mesaj döndürmeli")
    void analyzeTransaction_NoRiskReasons_ShouldReturnDefaultMessage() {
        // Given: Risk skoru var ama sebep yok
        BigDecimal riskScore = new BigDecimal("0.3");
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(riskScore);
        when(riskScoreCalculator.getRiskReasons(testTransaction)).thenReturn(Arrays.asList());
        
        // When: İşlem analiz edilir
        FraudResponse response = fraudDetectionService.analyzeTransaction(testTransaction);
        
        // Then: Varsayılan mesaj döndürülmeli
        assertEquals("Şüpheli desen tespit edilmedi", response.getReason(), 
                    "Risk sebebi yoksa varsayılan mesaj döndürülmeli");
    }
    
    @Test
    @DisplayName("Response'da transaction ID doğru set edilmeli")
    void analyzeTransaction_ShouldSetCorrectTransactionId() {
        // Given: Risk skoru
        BigDecimal riskScore = new BigDecimal("0.3");
        when(riskScoreCalculator.calculateFinalRiskScore(testTransaction)).thenReturn(riskScore);
        when(riskScoreCalculator.getRiskReasons(testTransaction)).thenReturn(Arrays.asList());
        
        // When: İşlem analiz edilir
        FraudResponse response = fraudDetectionService.analyzeTransaction(testTransaction);
        
        // Then: Transaction ID doğru set edilmeli
        assertEquals("TXN-001", response.getTransactionId(), 
                    "Transaction ID doğru set edilmeli");
    }
}


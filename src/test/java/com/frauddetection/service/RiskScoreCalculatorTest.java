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

        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN-001");
        testTransaction.setCardNumber("4111111111111111");
        testTransaction.setAmount(new BigDecimal("15000.0"));
        testTransaction.setTransactionTimestamp(LocalDateTime.now());

        List<FraudRule> fraudRules =
                Arrays.asList(timeOfDayRule, amountRule, frequencyRule);

        riskScoreCalculator = new RiskScoreCalculator(
                fraudRules,
                new BigDecimal("0.30"),
                new BigDecimal("0.35"),
                new BigDecimal("0.35")
        );
    }

    @Test
    @DisplayName("Ağırlıklı risk skoru doğru hesaplanmalı")
    void calculateFinalRiskScore_ShouldCalculateWeightedSum() {

        when(timeOfDayRule.calculateRisk(testTransaction)).thenReturn(0.8);
        when(amountRule.calculateRisk(testTransaction)).thenReturn(0.6);
        when(frequencyRule.calculateRisk(testTransaction)).thenReturn(0.0);

        BigDecimal riskScore =
                riskScoreCalculator.calculateFinalRiskScore(testTransaction);

        assertEquals(0.45, riskScore.doubleValue(), 0.01);

        verify(timeOfDayRule).calculateRisk(testTransaction);
        verify(amountRule).calculateRisk(testTransaction);
        verify(frequencyRule).calculateRisk(testTransaction);
    }

    @Test
    @DisplayName("Risk skoru 1.0'ı geçerse normalize edilmeli")
    void calculateFinalRiskScore_ShouldNormalizeToMax() {

        when(timeOfDayRule.calculateRisk(testTransaction)).thenReturn(1.0);
        when(amountRule.calculateRisk(testTransaction)).thenReturn(1.0);
        when(frequencyRule.calculateRisk(testTransaction)).thenReturn(1.0);

        BigDecimal riskScore =
                riskScoreCalculator.calculateFinalRiskScore(testTransaction);

        assertEquals(1.0, riskScore.doubleValue(), 0.01);
    }

    @Test
    @DisplayName("Risk skoru minimum 0.0 olmalı")
    void calculateFinalRiskScore_ShouldNormalizeToMin() {

        when(timeOfDayRule.calculateRisk(testTransaction)).thenReturn(0.0);
        when(amountRule.calculateRisk(testTransaction)).thenReturn(0.0);
        when(frequencyRule.calculateRisk(testTransaction)).thenReturn(0.0);

        BigDecimal riskScore =
                riskScoreCalculator.calculateFinalRiskScore(testTransaction);

        assertEquals(0.0, riskScore.doubleValue(), 0.01);
    }

    @Test
    @DisplayName("Null işlem için 0.0 dönmeli")
    void calculateFinalRiskScore_NullTransaction_ShouldReturnZero() {

        BigDecimal riskScore =
                riskScoreCalculator.calculateFinalRiskScore(null);

        assertEquals(0.0, riskScore.doubleValue());

        verify(timeOfDayRule, never()).calculateRisk(any());
        verify(amountRule, never()).calculateRisk(any());
        verify(frequencyRule, never()).calculateRisk(any());
    }

    @Test
    @DisplayName("Risk açıklamaları toplanmalı")
    void getRiskReasons_ShouldCollectAllReasons() {

        when(timeOfDayRule.getRiskReason(testTransaction)).thenReturn("Gece işlemi");
        when(amountRule.getRiskReason(testTransaction)).thenReturn("Yüksek tutar");
        when(frequencyRule.getRiskReason(testTransaction)).thenReturn(null);

        List<String> reasons =
                riskScoreCalculator.getRiskReasons(testTransaction);

        assertEquals(2, reasons.size());
        assertTrue(reasons.contains("Gece işlemi"));
        assertTrue(reasons.contains("Yüksek tutar"));
    }

    @Test
    @DisplayName("Hiç risk yoksa boş liste dönmeli")
    void getRiskReasons_NoRisks_ShouldReturnEmptyList() {

        when(timeOfDayRule.getRiskReason(testTransaction)).thenReturn(null);
        when(amountRule.getRiskReason(testTransaction)).thenReturn(null);
        when(frequencyRule.getRiskReason(testTransaction)).thenReturn(null);

        List<String> reasons =
                riskScoreCalculator.getRiskReasons(testTransaction);

        assertTrue(reasons.isEmpty());
    }
}
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
import java.util.stream.Collectors;

@Service
public class RiskScoreCalculator {

private static final Logger logger = LoggerFactory.getLogger(RiskScoreCalculator.class);

private final List<FraudRule> fraudRules;

private final BigDecimal timeWeight;
private final BigDecimal amountWeight;
private final BigDecimal frequencyWeight;

private static final BigDecimal MIN_RISK_SCORE = BigDecimal.ZERO;
private static final BigDecimal MAX_RISK_SCORE = BigDecimal.ONE;

private static final int SCALE = 4;

public RiskScoreCalculator(
        List<FraudRule> fraudRules,
        @Value("${fraud.detection.rules.time-of-day-weight:0.30}") BigDecimal timeWeight,
        @Value("${fraud.detection.rules.amount-weight:0.35}") BigDecimal amountWeight,
        @Value("${fraud.detection.rules.frequency-weight:0.35}") BigDecimal frequencyWeight) {

    this.fraudRules = fraudRules;
    this.timeWeight = timeWeight;
    this.amountWeight = amountWeight;
    this.frequencyWeight = frequencyWeight;

    logger.info("RiskScoreCalculator başlatıldı - {} kural yüklendi", fraudRules.size());
}

public BigDecimal calculateFinalRiskScore(Transaction transaction) {

    if (transaction == null) {
        logger.warn("Geçersiz işlem - risk skoru 0.0 döndürülüyor");
        return MIN_RISK_SCORE;
    }

    BigDecimal totalScore = BigDecimal.ZERO;

    for (int i = 0; i < fraudRules.size(); i++) {
        FraudRule rule = fraudRules.get(i);

        double risk = rule.calculateRisk(transaction);

        BigDecimal weight = BigDecimal.ZERO;

        String ruleName = rule.getRuleName();
        if (ruleName == null) {
            ruleName = "";
        }

        if (ruleName.contains("Gün")) {
            weight = timeWeight;
        } else if (ruleName.contains("Tutar")) {
            weight = amountWeight;
        } else if (ruleName.contains("Sıklık")) {
            weight = frequencyWeight;
        } else {
            // Fallback: varsayılan olarak ekleme sırasına göre metrikleri seç
            if (i == 0) {
                weight = timeWeight;
            } else if (i == 1) {
                weight = amountWeight;
            } else if (i == 2) {
                weight = frequencyWeight;
            }
        }

        BigDecimal weightedScore = BigDecimal.valueOf(risk)
                .multiply(weight)
                .setScale(SCALE, RoundingMode.HALF_UP);

        totalScore = totalScore.add(weightedScore);
    }

    BigDecimal normalizedScore = totalScore
            .max(MIN_RISK_SCORE)
            .min(MAX_RISK_SCORE)
            .setScale(SCALE, RoundingMode.HALF_UP);

    return normalizedScore;
}

public List<String> getRiskReasons(Transaction transaction) {

    return fraudRules.stream()
            .map(rule -> rule.getRiskReason(transaction))
            .filter(reason -> reason != null && !reason.isEmpty())
            .collect(Collectors.toList());
}
}
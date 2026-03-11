package com.frauddetection.dto;

import com.frauddetection.domain.FraudDecision;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dolandırıcılık analiz sonucu için API yanıt modeli.
 *
 * @author Dolandırıcılık Tespit Ekibi
 */
public class FraudResponse {

    private String transactionId;
    private FraudDecision decision;
    private BigDecimal riskScore;
    private String message;
    private String reason;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime analyzedAt;

    public FraudResponse() {
        this.analyzedAt = LocalDateTime.now();
    }

    public FraudResponse(String transactionId, FraudDecision decision, BigDecimal riskScore,
                         String message, String reason) {
        this.transactionId = transactionId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.message = message;
        this.reason = reason;
        this.analyzedAt = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public FraudDecision getDecision() {
        return decision;
    }

    public void setDecision(FraudDecision decision) {
        this.decision = decision;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}

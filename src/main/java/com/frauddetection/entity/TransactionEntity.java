package com.frauddetection.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_fraudulent", columnList = "fraudulent")
})
public class TransactionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "fraudulent", nullable = false)
    private Boolean fraudulent;
    
    @Column(name = "triggered_rule", length = 500)
    private String triggeredRule;
    
    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;
    
    @Column(name = "decision", length = 20)
    private String decision;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public TransactionEntity() {
    }
    
    public TransactionEntity(String accountId, BigDecimal amount, LocalDateTime timestamp, 
                           Boolean fraudulent, String triggeredRule) {
        this.accountId = accountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.fraudulent = fraudulent;
        this.triggeredRule = triggeredRule;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getFraudulent() {
        return fraudulent;
    }
    
    public void setFraudulent(Boolean fraudulent) {
        this.fraudulent = fraudulent;
    }
    
    public String getTriggeredRule() {
        return triggeredRule;
    }
    
    public void setTriggeredRule(String triggeredRule) {
        this.triggeredRule = triggeredRule;
    }
    
    public BigDecimal getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }
    
    public String getDecision() {
        return decision;
    }
    
    public void setDecision(String decision) {
        this.decision = decision;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


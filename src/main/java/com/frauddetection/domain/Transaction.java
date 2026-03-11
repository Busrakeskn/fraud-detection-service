package com.frauddetection.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain modeli - işlem (transaction) bilgilerini taşır.
 *
 * API katmanından gelen DTO'lar bu modele dönüştürülür
 * ve iş mantığı bu model üzerinden çalışır.
 *
 * @author Dolandırıcılık Tespit Ekibi
 */
public class Transaction {

    private String transactionId;
    private String cardNumber;
    private BigDecimal amount;
    private String merchantName;
    private String merchantCategory;
    private LocalDateTime transactionTimestamp;
    private String currency;
    private String cardHolderName;

    public Transaction() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(String merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }
}

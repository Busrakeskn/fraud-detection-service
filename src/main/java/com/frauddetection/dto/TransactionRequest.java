package com.frauddetection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API'den gelen işlem isteğini temsil eden DTO.
 *
 * Bu sınıf, HTTP isteğinden gelen verilerin doğrulanması ve
 * servis katmanına iletilmesi için kullanılır.
 *
 * @author Dolandırıcılık Tespit Ekibi
 */
public class TransactionRequest {

    @NotBlank(message = "Transaction ID boş olamaz")
    private String transactionId;

    @NotBlank(message = "Kart numarası boş olamaz")
    @Size(min = 13, max = 19, message = "Kart numarası 13-19 haneli olmalıdır")
    private String cardNumber;

    @NotNull(message = "Tutar boş olamaz")
    @Positive(message = "Tutar pozitif olmalıdır")
    private BigDecimal amount;

    private String merchantName;

    private String merchantCategory;

    @NotNull(message = "İşlem zamanı boş olamaz")
    private LocalDateTime transactionTimestamp;

    @NotBlank(message = "Para birimi boş olamaz")
    @Size(min = 3, max = 3, message = "Para birimi üç haneli olmalıdır")
    private String currency;

    private String cardHolderName;

    public TransactionRequest() {
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

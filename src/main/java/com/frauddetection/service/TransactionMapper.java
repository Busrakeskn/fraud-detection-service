package com.frauddetection.service;

import com.frauddetection.domain.Transaction;
import com.frauddetection.dto.TransactionRequest;
import org.springframework.stereotype.Component;

/**
 * DTO ve domain model arasında dönüştürme için mapper yardımcı sınıfı.
 * 
 * Bu sınıf, yalnızca katmanlar arasındaki mapping mantığını işleyerek
 * Tek Sorumluluk Prensibi'ni takip eder. API katmanı (DTO'lar) ve
 * iş katmanı (Domain modelleri) arasında temiz bir ayrım sağlar.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@Component
public class TransactionMapper {
    
    /**
     * TransactionRequest DTO'sunu Transaction domain modeline dönüştürür.
     * 
     * @param request API katmanından TransactionRequest
     * @return Transaction domain modeli
     */
    public Transaction toDomain(TransactionRequest request) {
        if (request == null) {
            return null;
        }
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(request.getTransactionId());
        transaction.setCardNumber(request.getCardNumber());
        transaction.setAmount(request.getAmount());
        transaction.setMerchantName(request.getMerchantName());
        transaction.setMerchantCategory(request.getMerchantCategory());
        transaction.setTransactionTimestamp(request.getTransactionTimestamp());
        transaction.setCurrency(request.getCurrency());
        transaction.setCardHolderName(request.getCardHolderName());
        
        return transaction;
    }
}

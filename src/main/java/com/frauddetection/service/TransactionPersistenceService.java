package com.frauddetection.service;

import com.frauddetection.domain.FraudDecision;
import com.frauddetection.domain.Transaction;
import com.frauddetection.domain.entity.TransactionEntity;
import com.frauddetection.dto.FraudResponse;
import com.frauddetection.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionPersistenceService.class);
    
    private final TransactionRepository transactionRepository;
    
    public TransactionPersistenceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional
    public void saveFraudAnalysisResult(Transaction transaction, FraudResponse response) {
        try {
            TransactionEntity entity = mapToEntity(transaction, response);
            transactionRepository.save(entity);
            
            logger.debug("İşlem veritabanına kaydedildi: {} - Karar: {}", 
                        transaction.getTransactionId(), response.getDecision());
        } catch (Exception e) {
            logger.error("İşlem veritabanına kaydedilirken hata: {}", 
                        transaction.getTransactionId(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionEntity> findByAccountId(String accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }
    
    private TransactionEntity mapToEntity(Transaction transaction, FraudResponse response) {
        TransactionEntity entity = new TransactionEntity();
        
        String accountId = transaction.getCardNumber() != null && transaction.getCardNumber().length() >= 4
                ? "****" + transaction.getCardNumber().substring(transaction.getCardNumber().length() - 4)
                : transaction.getTransactionId();
        
        entity.setAccountId(accountId);
        entity.setAmount(transaction.getAmount());
        entity.setTimestamp(transaction.getTransactionTimestamp());
        entity.setFraudulent(isFraudulent(response.getDecision()));
        entity.setTriggeredRule(response.getReason());
        entity.setRiskScore(response.getRiskScore());
        entity.setDecision(response.getDecision().name());
        
        return entity;
    }
    
    private Boolean isFraudulent(FraudDecision decision) {
        return decision == FraudDecision.REVIEW || decision == FraudDecision.BLOCK;
    }
}


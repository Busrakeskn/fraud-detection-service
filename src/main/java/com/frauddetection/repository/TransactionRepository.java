package com.frauddetection.repository;

import com.frauddetection.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    
    List<TransactionEntity> findByAccountId(String accountId);
    
    Page<TransactionEntity> findByAccountId(String accountId, Pageable pageable);
    
    List<TransactionEntity> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<TransactionEntity> findByFraudulent(Boolean fraudulent);
}


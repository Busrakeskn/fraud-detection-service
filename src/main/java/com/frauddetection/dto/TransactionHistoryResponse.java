package com.frauddetection.dto;

import com.frauddetection.entity.TransactionEntity;

import java.util.List;

public class TransactionHistoryResponse {
    
    private List<TransactionEntity> transactions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    
    public TransactionHistoryResponse(List<TransactionEntity> transactions, int page, int size, 
                                    long totalElements, int totalPages) {
        this.transactions = transactions;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
    
    public List<TransactionEntity> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(List<TransactionEntity> transactions) {
        this.transactions = transactions;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}


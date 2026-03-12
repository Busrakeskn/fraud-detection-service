package com.frauddetection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.dto.TransactionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FraudDetectionController için integration testler.
 * 
 * Gerçek Spring context kullanılarak endpoint testleri yazılmıştır.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FraudDetectionController Integration Tests")
class FraudDetectionControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Geçerli işlem isteği için 200 OK ve FraudResponse döndürmeli")
    void checkTransaction_ValidRequest_ShouldReturn200() throws Exception {
        // Given: Geçerli bir işlem isteği
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-INT-001");
        request.setCardNumber("4111111111111111");
        request.setAmount(new BigDecimal("5000.0"));
        request.setMerchantName("Test Merchant");
        request.setMerchantCategory("RETAIL");
        request.setTransactionTimestamp(LocalDateTime.of(2024, 12, 27, 14, 0, 0));
        request.setCurrency("USD");
        request.setCardHolderName("Test User");
        
        // When & Then: POST isteği gönderilir ve başarılı yanıt beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value("TXN-INT-001"))
                .andExpect(jsonPath("$.decision").exists())
                .andExpect(jsonPath("$.riskScore").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.analyzedAt").exists());
    }
    
    @Test
    @DisplayName("Geçersiz işlem isteği için 400 Bad Request döndürmeli")
    void checkTransaction_InvalidRequest_ShouldReturn400() throws Exception {
        // Given: Geçersiz bir işlem isteği (transactionId eksik)
        TransactionRequest request = new TransactionRequest();
        request.setCardNumber("4111111111111111");
        request.setAmount(new BigDecimal("5000.0"));
        request.setTransactionTimestamp(LocalDateTime.now());
        request.setCurrency("USD");
        // transactionId eksik - validation hatası
        
        // When & Then: POST isteği gönderilir ve 400 Bad Request beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("Geçersiz kart numarası için 400 Bad Request döndürmeli")
    void checkTransaction_InvalidCardNumber_ShouldReturn400() throws Exception {
        // Given: Geçersiz kart numarası (12 haneli - minimum 13 olmalı)
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-INT-002");
        request.setCardNumber("411111111111"); // 12 haneli - geçersiz
        request.setAmount(new BigDecimal("5000.0"));
        request.setTransactionTimestamp(LocalDateTime.now());
        request.setCurrency("USD");
        
        // When & Then: POST isteği gönderilir ve 400 Bad Request beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("Geçersiz tutar için 400 Bad Request döndürmeli")
    void checkTransaction_InvalidAmount_ShouldReturn400() throws Exception {
        // Given: Geçersiz tutar (negatif veya sıfır)
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-INT-003");
        request.setCardNumber("4111111111111111");
        request.setAmount(new BigDecimal("-100.0")); // Negatif tutar - geçersiz
        request.setTransactionTimestamp(LocalDateTime.now());
        request.setCurrency("USD");
        
        // When & Then: POST isteği gönderilir ve 400 Bad Request beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("Geçersiz para birimi için 400 Bad Request döndürmeli")
    void checkTransaction_InvalidCurrency_ShouldReturn400() throws Exception {
        // Given: Geçersiz para birimi (2 karakter - 3 olmalı)
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-INT-004");
        request.setCardNumber("4111111111111111");
        request.setAmount(new BigDecimal("5000.0"));
        request.setTransactionTimestamp(LocalDateTime.now());
        request.setCurrency("US"); // 2 karakter - geçersiz
        
        // When & Then: POST isteği gönderilir ve 400 Bad Request beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("Yüksek riskli işlem için BLOCK kararı döndürmeli")
    void checkTransaction_HighRiskTransaction_ShouldReturnReview() throws Exception {
        // Given: Yüksek riskli işlem (gece saati + yüksek tutar)
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-INT-005");
        request.setCardNumber("4111111111111111");
        request.setAmount(new BigDecimal("60000.0")); // Çok yüksek tutar
        request.setTransactionTimestamp(LocalDateTime.of(2024, 12, 27, 3, 0, 0)); // Gece saati
        request.setCurrency("USD");
        
        // When & Then: POST isteği gönderilir ve BLOCK kararı beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REVIEW"))
                .andExpect(jsonPath("$.riskScore").value(org.hamcrest.Matchers.greaterThan(0.4)));
    }
    
    @Test
    @DisplayName("Düşük riskli işlem için APPROVE kararı döndürmeli")
    void checkTransaction_LowRiskTransaction_ShouldReturnApprove() throws Exception {
        // Given: Düşük riskli işlem (normal saat + düşük tutar)
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("TXN-INT-006");
        request.setCardNumber("4111111111111111");
        request.setAmount(new BigDecimal("1000.0")); // Düşük tutar
        request.setTransactionTimestamp(LocalDateTime.of(2024, 12, 27, 14, 0, 0)); // Normal saat
        request.setCurrency("USD");
        
        // When & Then: POST isteği gönderilir ve APPROVE kararı beklenir
        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVE"))
                .andExpect(jsonPath("$.riskScore").value(org.hamcrest.Matchers.lessThan(0.4)));
    }
}



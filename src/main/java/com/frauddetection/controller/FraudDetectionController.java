package com.frauddetection.controller;

import com.frauddetection.domain.Transaction;
import com.frauddetection.dto.FraudResponse;
import com.frauddetection.dto.TransactionHistoryResponse;
import com.frauddetection.dto.TransactionRequest;
import com.frauddetection.service.FraudDetectionService;
import com.frauddetection.service.TransactionMapper;
import com.frauddetection.service.TransactionPersistenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dolandırıcılık tespit işlemleri için REST Controller.
 * 
 * Bu controller yalnızca HTTP katmanı sorumluluklarından sorumludur:
 * - İstek doğrulama
 * - Yanıt formatlama
 * - HTTP durum kodları
 * 
 * İş mantığı, Tek Sorumluluk Prensibi'ni takip ederek
 * servis katmanına devredilmiştir.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@RestController
@RequestMapping("/api/fraud")
@Tag(name = "Fraud Detection", description = "API for real-time credit card fraud detection")
public class FraudDetectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionController.class);
    
    private final FraudDetectionService fraudDetectionService;
    private final TransactionMapper transactionMapper;
    private final TransactionPersistenceService persistenceService;
    
    public FraudDetectionController(FraudDetectionService fraudDetectionService,
                                    TransactionMapper transactionMapper,
                                    TransactionPersistenceService persistenceService) {
        this.fraudDetectionService = fraudDetectionService;
        this.transactionMapper = transactionMapper;
        this.persistenceService = persistenceService;
    }
    
    /**
     * Bir işlemi dolandırıcılık için kontrol eden POST endpoint'i.
     * 
     * Bu endpoint bir işlem isteği alır, doğrular ve
     * risk skoru ve karar içeren bir dolandırıcılık analiz sonucu döner.
     * 
     * @param request Doğrulama ile işlem isteği
     * @return Risk skoru ve karar içeren FraudResponse
     */
    @PostMapping(value = "/check", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Check transaction for fraud",
            description = "Analyzes a banking transaction and returns fraud risk assessment. " +
                         "Returns risk score (0.0-1.0) and decision (APPROVE, REVIEW, BLOCK)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Fraud analysis completed successfully",
                    content = @Content(schema = @Schema(implementation = FraudResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    public ResponseEntity<FraudResponse> checkTransaction(
            @Valid @RequestBody TransactionRequest request) {
        
        logger.info("İşlem için dolandırıcılık kontrol isteği alındı: {}", request.getTransactionId());
        
        try {
            // DTO'yu domain modeline dönüştür
            Transaction transaction = transactionMapper.toDomain(request);
            
            // Dolandırıcılık analizini gerçekleştir
            FraudResponse response = fraudDetectionService.analyzeTransaction(transaction);
            
            logger.info("İşlem için dolandırıcılık kontrolü tamamlandı: {} - Karar: {}, Risk Skoru: {}", 
                       request.getTransactionId(), response.getDecision(), response.getRiskScore());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("İşlem için dolandırıcılık kontrolü işlenirken hata: {}", 
                        request.getTransactionId(), e);
            throw e; // Global exception handler'ın işlemesine izin ver
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Dolandırıcılık Tespit Servisi çalışıyor");
    }
    
    @GetMapping("/transactions/{accountId}")
    @Operation(
            summary = "Get transaction history by account ID",
            description = "Returns paginated transaction history for a specific account ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionHistoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            )
    })
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("İşlem geçmişi isteği alındı - Account ID: {}, Page: {}, Size: {}", accountId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<com.frauddetection.entity.TransactionEntity> transactionPage = 
                persistenceService.findByAccountId(accountId, pageable);
        
        TransactionHistoryResponse response = new TransactionHistoryResponse(
                transactionPage.getContent(),
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
        
        logger.info("İşlem geçmişi döndürüldü - Account ID: {}, Toplam: {}", 
                   accountId, transactionPage.getTotalElements());
        
        return ResponseEntity.ok(response);
    }
}

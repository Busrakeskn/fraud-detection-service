package com.frauddetection.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Uygulama için global exception handler.
 * 
 * Bu sınıf, tüm controller'lar genelinde merkezi exception handling sağlar.
 * Tutarlı hata yanıt formatı ve uygun HTTP durum kodları sağlar.
 * 
 * Tek Sorumluluk Prensibi'ni takip ederek, bu sınıf yalnızca
 * exception handling ve hata yanıt formatlamadan sorumludur.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * @Valid anotasyonlarından gelen doğrulama hatalarını işler.
     * 
     * @param ex MethodArgumentNotValidException
     * @return Doğrulama detayları ile hata yanıtı
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "İstek doğrulaması başarısız",
                errors.toString(),
                LocalDateTime.now()
        );
        
        logger.warn("Doğrulama hatası: {}", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Geçersiz argüman exception'larını işler.
     * 
     * @param ex IllegalArgumentException
     * @return Hata yanıtı
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "ILLEGAL_ARGUMENT",
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );
        
        logger.warn("Geçersiz argüman: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Diğer tüm işlenmemiş exception'ları işler.
     * 
     * @param ex Exception
     * @return Hata yanıtı
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        
        logger.error("Beklenmeyen hata oluştu", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "İstek işlenirken beklenmeyen bir hata oluştu",
                ex.getMessage(),
                LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Standart hata yanıt yapısı.
     */
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String details;
        private LocalDateTime timestamp;
        
        public ErrorResponse(String errorCode, String message, String details, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public String getErrorCode() {
            return errorCode;
        }
        
        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getDetails() {
            return details;
        }
        
        public void setDetails(String details) {
            this.details = details;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}

package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FrequencyRule Unit Tests")
class FrequencyRuleTest {

    private FrequencyRule frequencyRule;

    @BeforeEach
    void setUp() {
        // 15 dakika pencere, 3 işlemden sonra risk
        frequencyRule = new FrequencyRule(15, 3);
    }

    @Test
    @DisplayName("İlk işlem için düşük risk döndürmeli")
    void calculateRisk_FirstTransaction_ShouldReturnLowRisk() {

        LocalDateTime now = LocalDateTime.of(2024, 12, 27, 10, 0, 0);
        Transaction transaction = createTransaction("TXN-001", "4111111111111111", now);

        double riskScore = frequencyRule.calculateRisk(transaction);

        assertEquals(0.0, riskScore, "İlk işlem için risk skoru 0.0 olmalı");
    }

    @Test
    @DisplayName("3 işlem kısa sürede yapılırsa yüksek risk döndürmeli")
    void calculateRisk_ThreeTransactions_ShouldReturnHighRisk() {

        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);

        Transaction t1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(5));
        Transaction t2 = createTransaction("TXN-002", cardNumber, baseTime.minusMinutes(3));

        frequencyRule.calculateRisk(t1);
        frequencyRule.calculateRisk(t2);

        Transaction t3 = createTransaction("TXN-003", cardNumber, baseTime);

        double riskScore = frequencyRule.calculateRisk(t3);

        // 3 işlem → 0.4 risk
        assertEquals(0.4, riskScore, 0.01,
                "3 işlem kısa sürede gerçekleştiğinde risk skoru 0.4 olmalı");
    }

    @Test
    @DisplayName("2 işlem varsa risk oluşmamalı")
    void calculateRisk_TwoTransactions_ShouldReturnLowRisk() {

        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);

        Transaction t1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(5));
        frequencyRule.calculateRisk(t1);

        Transaction t2 = createTransaction("TXN-002", cardNumber, baseTime);

        double riskScore = frequencyRule.calculateRisk(t2);

        assertEquals(0.0, riskScore, 0.01,
                "2 işlem varsa risk skoru 0.0 olmalı");
    }

    @Test
    @DisplayName("Zaman penceresi dışındaki işlem risk üretmemeli")
    void calculateRisk_TransactionOutsideTimeWindow_ShouldReturnLowRisk() {

        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);

        Transaction t1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(20));
        frequencyRule.calculateRisk(t1);

        Transaction t2 = createTransaction("TXN-002", cardNumber, baseTime);

        double riskScore = frequencyRule.calculateRisk(t2);

        assertEquals(0.0, riskScore,
                "Zaman penceresi dışındaki işlem için risk oluşmamalı");
    }

    @Test
    @DisplayName("Null işlem için sıfır risk döndürmeli")
    void calculateRisk_NullTransaction_ShouldReturnZero() {

        double riskScore = frequencyRule.calculateRisk(null);

        assertEquals(0.0, riskScore);
    }

    @Test
    @DisplayName("Null kart numarası için sıfır risk döndürmeli")
    void calculateRisk_NullCardNumber_ShouldReturnZero() {

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-001");
        transaction.setCardNumber(null);
        transaction.setTransactionTimestamp(LocalDateTime.now());

        double riskScore = frequencyRule.calculateRisk(transaction);

        assertEquals(0.0, riskScore);
    }

    @Test
    @DisplayName("Kural adı doğru döndürülmeli")
    void getRuleName_ShouldReturnCorrectName() {

        String ruleName = frequencyRule.getRuleName();

        assertEquals("Sıklık Kuralı", ruleName);
    }

    @Test
    @DisplayName("Yüksek sıklık için risk sebebi döndürmeli")
    void getRiskReason_HighFrequency_ShouldReturnReason() {

        String cardNumber = "4111111111111111";
        LocalDateTime baseTime = LocalDateTime.of(2024, 12, 27, 10, 0, 0);

        Transaction t1 = createTransaction("TXN-001", cardNumber, baseTime.minusMinutes(5));
        Transaction t2 = createTransaction("TXN-002", cardNumber, baseTime.minusMinutes(3));

        frequencyRule.calculateRisk(t1);
        frequencyRule.calculateRisk(t2);

        Transaction t3 = createTransaction("TXN-003", cardNumber, baseTime);

        String reason = frequencyRule.getRiskReason(t3);

        assertNotNull(reason);
        assertTrue(reason.contains("çoklu işlem"));
    }

    private Transaction createTransaction(String transactionId, String cardNumber,
                                          LocalDateTime timestamp) {

        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setCardNumber(cardNumber);
        transaction.setTransactionTimestamp(timestamp);

        return transaction;
    }
}
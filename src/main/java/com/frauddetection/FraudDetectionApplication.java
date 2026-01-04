package com.frauddetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gerçek Zamanlı Kredi Kartı Dolandırıcılık Tespit Servisi için ana uygulama sınıfı.
 * 
 * Bu servis, bankacılık işlemlerini analiz etmek ve kural tabanlı algoritmalar kullanarak
 * potansiyel dolandırıcılık faaliyetlerini tespit etmek için REST API endpoint'leri sağlar.
 * 
 * Modüler mimari:
 * - FraudRule interface ile bağımsız kurallar
 * - RiskScoreCalculator ile ağırlıklı risk hesaplama
 * - FraudDetectionService ile orkestrasyon
 * 
 * @author Dolandırıcılık Tespit Ekibi
 * @version 1.0.0
 */
@SpringBootApplication
public class FraudDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionApplication.class, args);
    }
}

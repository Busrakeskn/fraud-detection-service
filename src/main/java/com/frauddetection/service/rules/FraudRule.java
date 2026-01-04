package com.frauddetection.service.rules;

import com.frauddetection.domain.Transaction;

/**
 * Dolandırıcılık tespit kuralları için arayüz.
 * 
 * Strategy Pattern kullanılarak her kural bağımsız bir strateji olarak
 * uygulanır. Bu sayede kurallar birbirinden bağımsızdır ve kolayca
 * eklenip çıkarılabilir.
 * 
 * Her kural, bir işlemi analiz eder ve 0.0 ile 1.0 arasında
 * bir risk skoru döndürür.
 * 
 * @author Dolandırıcılık Tespit Ekibi
 */
public interface FraudRule {
    
    /**
     * İşlemi analiz eder ve risk skorunu hesaplar.
     * 
     * @param transaction Analiz edilecek işlem
     * @return 0.0 (düşük risk) ile 1.0 (yüksek risk) arasında risk skoru
     */
    double calculateRisk(Transaction transaction);
    
    /**
     * Kuralın adını döndürür.
     * Loglama ve açıklama amaçlı kullanılır.
     * 
     * @return Kural adı
     */
    String getRuleName();
    
    /**
     * Kuralın risk katkısını açıklayan mesaj döndürür.
     * Eğer kural risk tespit ettiyse açıklayıcı mesaj, aksi halde null döner.
     * 
     * @param transaction Analiz edilen işlem
     * @return Risk açıklaması veya null
     */
    String getRiskReason(Transaction transaction);
}



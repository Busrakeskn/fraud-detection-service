package com.frauddetection.domain;

/**
 * Dolandırıcılık kararlarını temsil eden enum.
 *
 * @author Dolandırıcılık Tespit Ekibi
 */
public enum FraudDecision {
    APPROVE("İşlem onaylandı"),
    REVIEW("İnceleme gerektiriyor"),
    BLOCK("İşlem engellendi");

    private final String description;

    FraudDecision(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

package com.sitepulse.engine.metrics.domain.enums;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public String toPersistenceValue() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    public static RiskLevel fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        return valueOf(value.toUpperCase());
    }
}

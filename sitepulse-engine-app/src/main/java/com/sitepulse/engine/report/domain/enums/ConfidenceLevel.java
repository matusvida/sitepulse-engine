package com.sitepulse.engine.report.domain.enums;

public enum ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW;

    public String toPersistenceValue() {
        return name().toLowerCase();
    }

    public static ConfidenceLevel fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        return valueOf(value.toUpperCase());
    }
}

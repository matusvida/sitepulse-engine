package com.sitepulse.engine.alert.domain.enums;

public enum AlertSeverity {
    LOW,
    MEDIUM,
    HIGH;

    public static AlertSeverity fromValue(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        return AlertSeverity.valueOf(value.trim().toUpperCase());
    }

    public String toPersistenceValue() {
        return name().toLowerCase();
    }
}

package com.sitepulse.engine.alert.domain.enums;

public enum AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED;

    public static AlertStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        return AlertStatus.valueOf(value.trim().toUpperCase());
    }

    public String toPersistenceValue() {
        return name().toLowerCase();
    }
}

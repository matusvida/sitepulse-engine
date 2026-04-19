package com.sitepulse.engine.metrics.domain.enums;

import java.util.Locale;

public enum DailyActivityStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    UNKNOWN("unknown");

    private final String persistenceValue;

    DailyActivityStatus(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }

    public static DailyActivityStatus fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DailyActivityStatus status : values()) {
            if (status.persistenceValue.equals(normalized)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}

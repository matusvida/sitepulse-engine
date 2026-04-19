package com.sitepulse.engine.metrics.domain.enums;

import java.util.Locale;

public enum DailyObservationConfidence {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String persistenceValue;

    DailyObservationConfidence(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }

    public static DailyObservationConfidence fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DailyObservationConfidence confidence : values()) {
            if (confidence.persistenceValue.equals(normalized)) {
                return confidence;
            }
        }
        return LOW;
    }
}

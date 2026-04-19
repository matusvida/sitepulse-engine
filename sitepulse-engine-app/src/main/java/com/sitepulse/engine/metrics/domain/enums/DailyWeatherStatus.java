package com.sitepulse.engine.metrics.domain.enums;

import java.util.Locale;

public enum DailyWeatherStatus {
    CLEAR_OR_NORMAL("clear_or_normal"),
    RAIN("rain"),
    SNOW("snow"),
    UNCLEAR("unclear");

    private final String persistenceValue;

    DailyWeatherStatus(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }

    public static DailyWeatherStatus fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return UNCLEAR;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DailyWeatherStatus status : values()) {
            if (status.persistenceValue.equals(normalized)) {
                return status;
            }
        }
        return UNCLEAR;
    }
}

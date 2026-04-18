package com.sitepulse.engine.report.domain.enums;

import java.util.Locale;

public enum WeatherSummary {
    CLEAR("clear"),
    OVERCAST("overcast"),
    RAIN("rain"),
    WET("wet"),
    FOG("fog"),
    SNOW("snow"),
    MIXED("mixed"),
    UNCLEAR("unclear");

    private final String persistenceValue;

    WeatherSummary(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }

    public static WeatherSummary fromObservation(String observation) {
        if (observation == null || observation.isBlank()) {
            return UNCLEAR;
        }
        String normalized = observation.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("fog")) {
            return FOG;
        }
        if (normalized.contains("snow") || normalized.contains("ice")) {
            return SNOW;
        }
        if (normalized.contains("rain") || normalized.contains("shower") || normalized.contains("drizzle")) {
            return RAIN;
        }
        if (normalized.contains("wet")) {
            return WET;
        }
        if (normalized.contains("overcast") || normalized.contains("cloud")) {
            return OVERCAST;
        }
        if (normalized.contains("clear") || normalized.contains("sun")) {
            return CLEAR;
        }
        return UNCLEAR;
    }
}

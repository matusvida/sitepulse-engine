package com.sitepulse.engine.report.domain.enums;

public enum GenerationOrigin {
    AUTOMATIC,
    MANUAL;

    public String toPersistenceValue() {
        return name().toLowerCase();
    }

    public static GenerationOrigin fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return MANUAL;
        }
        return valueOf(value.toUpperCase());
    }
}

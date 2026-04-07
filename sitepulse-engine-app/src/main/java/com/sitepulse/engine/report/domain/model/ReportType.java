package com.sitepulse.engine.report.domain.model;

public enum ReportType {
    CUSTOM,
    WEEKLY,
    DAILY;

    public String toPersistenceValue() {
        return name().toLowerCase();
    }

    public static ReportType fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOM;
        }
        return valueOf(value.toUpperCase());
    }
}

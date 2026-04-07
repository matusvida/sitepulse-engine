package com.sitepulse.engine.plan.domain.model;

public enum PlanStatus {
    PROCESSING,
    READY,
    FAILED;

    public static PlanStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PROCESSING;
        }
        return PlanStatus.valueOf(value.trim().toUpperCase());
    }

    public String toPersistenceValue() {
        return name().toLowerCase();
    }
}

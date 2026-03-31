package com.sitepulse.engine.plan.domain.model;

public enum MilestoneStatus {
    NOT_STARTED,
    ON_TRACK,
    DELAYED,
    COMPLETED;

    public static MilestoneStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NOT_STARTED;
        }
        return MilestoneStatus.valueOf(value.trim().toUpperCase());
    }

    public String toPersistenceValue() {
        return name().toLowerCase();
    }
}

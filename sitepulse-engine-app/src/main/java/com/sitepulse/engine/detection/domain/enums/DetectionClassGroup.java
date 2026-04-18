package com.sitepulse.engine.detection.domain.enums;

import java.util.Locale;

public enum DetectionClassGroup {
    PEOPLE("people"),
    LIGHT_VEHICLE("light_vehicle"),
    TRUCK("truck"),
    TRANSPORT("transport"),
    EARTHMOVING("earthmoving"),
    LIFTING("lifting"),
    PAVING("paving"),
    STRUCTURE("structure"),
    POWER("power"),
    AERIAL("aerial"),
    OTHER_VEHICLE("other_vehicle"),
    OTHER_EQUIPMENT("other_equipment"),
    UNKNOWN("unknown");

    private final String persistenceValue;

    DetectionClassGroup(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }

    public static DetectionClassGroup fromPersistenceValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DetectionClassGroup group : values()) {
            if (group.persistenceValue.equals(normalized)) {
                return group;
            }
        }
        return UNKNOWN;
    }
}

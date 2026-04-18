package com.sitepulse.engine.detection.application.enums;

public enum ImageEvidenceCandidateTag {
    UPPER_PARKING_ACTIVITY("upper_parking_activity"),
    LOADING_ACTIVITY("loading_activity"),
    PEAK_ACTIVITY("peak_activity"),
    NEW_EQUIPMENT("new_equipment"),
    EQUIPMENT_CHANGE("equipment_change"),
    QUALITY_LIMITED("quality_limited");

    private final String value;

    ImageEvidenceCandidateTag(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

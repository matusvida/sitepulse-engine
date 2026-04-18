package com.sitepulse.engine.detection.application.enums;

public enum ImageEvidenceSummaryField {
    CLASS_COUNTS("class_counts"),
    DOMINANT_CLASSES("dominant_classes"),
    FIRST_APPEARANCE_FLAGS("first_appearance_flags"),
    CHANGE_FLAGS("change_flags"),
    QUALITY_FLAGS("quality_flags"),
    CANDIDATE_TAGS("candidate_tags");

    private final String key;

    ImageEvidenceSummaryField(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}

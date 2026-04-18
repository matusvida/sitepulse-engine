package com.sitepulse.engine.detection.application.enums;

public enum ImageEvidenceFirstAppearanceFlag {
    FIRST("first_");

    private final String prefix;

    ImageEvidenceFirstAppearanceFlag(String prefix) {
        this.prefix = prefix;
    }

    public String format(String className) {
        return prefix + className;
    }
}

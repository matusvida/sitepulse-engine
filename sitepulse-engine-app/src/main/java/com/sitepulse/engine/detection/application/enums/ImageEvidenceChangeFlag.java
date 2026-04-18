package com.sitepulse.engine.detection.application.enums;

public enum ImageEvidenceChangeFlag {
    MORE("more_"),
    LESS("less_");

    private final String prefix;

    ImageEvidenceChangeFlag(String prefix) {
        this.prefix = prefix;
    }

    public String format(String className) {
        return prefix + className;
    }
}

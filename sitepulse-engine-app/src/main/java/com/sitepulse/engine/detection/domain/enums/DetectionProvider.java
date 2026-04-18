package com.sitepulse.engine.detection.domain.enums;

import java.util.Locale;

public enum DetectionProvider {
    OPENAI,
    YOLO;

    public static DetectionProvider from(String value) {
        if (value == null || value.isBlank()) {
            return OPENAI;
        }
        return DetectionProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

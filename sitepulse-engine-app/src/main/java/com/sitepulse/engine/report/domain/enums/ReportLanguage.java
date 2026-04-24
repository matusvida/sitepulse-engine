package com.sitepulse.engine.report.domain.enums;

import com.sitepulse.engine.common.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public enum ReportLanguage {
    SK("sk", "Slovak"),
    EN("en", "English");

    private final String persistenceValue;
    private final String promptLabel;

    ReportLanguage(String persistenceValue, String promptLabel) {
        this.persistenceValue = persistenceValue;
        this.promptLabel = promptLabel;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }

    public String promptLabel() {
        return promptLabel;
    }

    public static ReportLanguage fromRequest(String value) {
        if (value == null || value.isBlank()) {
            return SK;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SK" -> SK;
            case "EN" -> EN;
            default -> throw new ValidationException("Unsupported report language: " + value + ". Use SK or EN.");
        };
    }

    public static ReportLanguage fromPersistenceValue(String value) {
        if (StringUtils.isBlank(value)) {
            return SK;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "sk" -> SK;
            case "en" -> EN;
            default -> throw new IllegalArgumentException("Unsupported report language persistence value: " + value);
        };
    }
}

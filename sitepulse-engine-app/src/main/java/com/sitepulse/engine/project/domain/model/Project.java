package com.sitepulse.engine.project.domain.model;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Project {

    private final Integer id;
    private String name;
    private String location;
    private String storageKeyPrefix;
    private String timezone;
    private final OffsetDateTime createdAt;

    private Project(Integer id, String name, String location, String storageKeyPrefix, String timezone, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.storageKeyPrefix = normalize(storageKeyPrefix);
        this.timezone = normalizeTimezone(timezone);
        this.createdAt = createdAt;
    }

    public static Project create(String name, String location, String storageKeyPrefix, String timezone, OffsetDateTime createdAt) {
        return new Project(null, name, normalize(location), storageKeyPrefix, timezone, createdAt);
    }

    public static Project restore(Integer id, String name, String location, String storageKeyPrefix, String timezone, OffsetDateTime createdAt) {
        return new Project(id, name, location, storageKeyPrefix, timezone, createdAt);
    }

    public void update(String name, String location, String storageKeyPrefix, String timezone) {
        if (name != null) {
            this.name = name;
        }
        if (location != null) {
            this.location = location;
        }
        if (storageKeyPrefix != null) {
            this.storageKeyPrefix = normalize(storageKeyPrefix);
        }
        if (timezone != null) {
            this.timezone = normalizeTimezone(timezone);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String normalizeTimezone(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return ZoneId.of(normalized).getId();
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid timezone: " + normalized, ex);
        }
    }
}

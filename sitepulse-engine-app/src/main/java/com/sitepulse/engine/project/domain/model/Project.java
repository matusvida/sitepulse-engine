package com.sitepulse.engine.project.domain.model;

import java.time.OffsetDateTime;
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
    private final OffsetDateTime createdAt;

    private Project(Integer id, String name, String location, String storageKeyPrefix, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.storageKeyPrefix = normalize(storageKeyPrefix);
        this.createdAt = createdAt;
    }

    public static Project create(String name, String location, String storageKeyPrefix, OffsetDateTime createdAt) {
        return new Project(null, name, normalize(location), storageKeyPrefix, createdAt);
    }

    public static Project restore(Integer id, String name, String location, String storageKeyPrefix, OffsetDateTime createdAt) {
        return new Project(id, name, location, storageKeyPrefix, createdAt);
    }

    public void update(String name, String location, String storageKeyPrefix) {
        if (name != null) {
            this.name = name;
        }
        if (location != null) {
            this.location = location;
        }
        if (storageKeyPrefix != null) {
            this.storageKeyPrefix = normalize(storageKeyPrefix);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

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
    private String dropboxPath;
    private final OffsetDateTime createdAt;

    private Project(Integer id, String name, String location, String dropboxPath, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.dropboxPath = dropboxPath;
        this.createdAt = createdAt;
    }

    public static Project create(String name, String location, String dropboxPath, OffsetDateTime createdAt) {
        return new Project(null, name, normalize(location), normalize(dropboxPath), createdAt);
    }

    public static Project restore(Integer id, String name, String location, String dropboxPath, OffsetDateTime createdAt) {
        return new Project(id, name, location, dropboxPath, createdAt);
    }

    public void update(String name, String location, String dropboxPath) {
        if (name != null) {
            this.name = name;
        }
        if (location != null) {
            this.location = location;
        }
        if (dropboxPath != null) {
            this.dropboxPath = dropboxPath;
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

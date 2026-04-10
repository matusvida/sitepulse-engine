package com.sitepulse.engine.project.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Camera {

    private final Integer id;
    private final Integer projectId;
    private final String name;
    private List<List<Double>> roiPolygon;
    private Boolean dropOutside;
    private String dropboxPath;
    private String keyPrefix;
    private final Integer imageWidth;
    private final Integer imageHeight;
    private final OffsetDateTime createdAt;

    private Camera(
            Integer id,
            Integer projectId,
            String name,
            List<List<Double>> roiPolygon,
            Boolean dropOutside,
            String dropboxPath,
            String keyPrefix,
            Integer imageWidth,
            Integer imageHeight,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.roiPolygon = roiPolygon;
        this.dropOutside = dropOutside;
        this.dropboxPath = normalize(dropboxPath);
        this.keyPrefix = normalizeKeyPrefix(keyPrefix, name);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.createdAt = createdAt;
    }

    public static Camera create(
            Integer projectId,
            String name,
            String dropboxPath,
            String keyPrefix,
            List<List<Double>> roiPolygon,
            Boolean dropOutside,
            Integer imageWidth,
            Integer imageHeight,
            OffsetDateTime createdAt
    ) {
        return new Camera(null, projectId, name, roiPolygon, dropOutside == null ? Boolean.TRUE : dropOutside, dropboxPath, keyPrefix, imageWidth, imageHeight, createdAt);
    }

    public static Camera restore(
            Integer id,
            Integer projectId,
            String name,
            List<List<Double>> roiPolygon,
            Boolean dropOutside,
            String dropboxPath,
            String keyPrefix,
            Integer imageWidth,
            Integer imageHeight,
            OffsetDateTime createdAt
    ) {
        return new Camera(id, projectId, name, roiPolygon, dropOutside, dropboxPath, keyPrefix, imageWidth, imageHeight, createdAt);
    }

    public void update(String dropboxPath, String keyPrefix, List<List<Double>> roiPolygon, Boolean dropOutside) {
        if (dropboxPath != null) {
            this.dropboxPath = normalize(dropboxPath);
        }
        if (keyPrefix != null) {
            this.keyPrefix = normalizeKeyPrefix(keyPrefix, this.name);
        }
        if (roiPolygon != null) {
            this.roiPolygon = roiPolygon;
        }
        if (dropOutside != null) {
            this.dropOutside = dropOutside;
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String normalizeKeyPrefix(String keyPrefix, String name) {
        String normalized = normalize(keyPrefix);
        if (normalized != null) {
            return trimSlashes(normalized);
        }
        String fallback = slugify(name);
        return fallback == null || fallback.isBlank() ? null : fallback;
    }

    private static String slugify(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        String slug = normalized.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? null : slug;
    }

    private static String trimSlashes(String value) {
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}

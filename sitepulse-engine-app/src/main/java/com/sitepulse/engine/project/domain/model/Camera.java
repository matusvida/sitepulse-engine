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
    private final String keyPrefix;
    private final Integer imageWidth;
    private final Integer imageHeight;
    private final OffsetDateTime createdAt;

    private Camera(
            Integer id,
            Integer projectId,
            String name,
            List<List<Double>> roiPolygon,
            Boolean dropOutside,
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
        this.keyPrefix = keyPrefix;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.createdAt = createdAt;
    }

    public static Camera create(
            Integer projectId,
            String name,
            String keyPrefix,
            List<List<Double>> roiPolygon,
            Boolean dropOutside,
            Integer imageWidth,
            Integer imageHeight,
            OffsetDateTime createdAt
    ) {
        return new Camera(null, projectId, name, roiPolygon, dropOutside == null ? Boolean.TRUE : dropOutside, normalize(keyPrefix), imageWidth, imageHeight, createdAt);
    }

    public static Camera restore(
            Integer id,
            Integer projectId,
            String name,
            List<List<Double>> roiPolygon,
            Boolean dropOutside,
            String keyPrefix,
            Integer imageWidth,
            Integer imageHeight,
            OffsetDateTime createdAt
    ) {
        return new Camera(id, projectId, name, roiPolygon, dropOutside, keyPrefix, imageWidth, imageHeight, createdAt);
    }

    public void update(List<List<Double>> roiPolygon, Boolean dropOutside) {
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
}

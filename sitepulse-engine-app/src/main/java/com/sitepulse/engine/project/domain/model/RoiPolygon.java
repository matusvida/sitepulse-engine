package com.sitepulse.engine.project.domain.model;

import java.util.List;

public record RoiPolygon(List<List<Double>> points) {
    public RoiPolygon {
        if (points != null && points.size() < 3) {
            throw new IllegalArgumentException("ROI polygon must have at least 3 points");
        }
    }
}

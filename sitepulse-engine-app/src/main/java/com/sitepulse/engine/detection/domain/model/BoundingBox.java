package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record BoundingBox(List<Double> xyxy) {
    public BoundingBox {
        if (xyxy == null || xyxy.size() != 4) {
            throw new IllegalArgumentException("Bounding box must have exactly 4 coordinates");
        }
        xyxy = List.copyOf(xyxy);
    }

    public double x1() { return xyxy.get(0); }
    public double y1() { return xyxy.get(1); }
    public double x2() { return xyxy.get(2); }
    public double y2() { return xyxy.get(3); }
}

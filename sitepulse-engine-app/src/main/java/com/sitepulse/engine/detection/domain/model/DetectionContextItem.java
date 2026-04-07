package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record DetectionContextItem(
        Integer trackId,
        Integer classId,
        String className,
        List<Double> bboxXyxy,
        String colorHint
) {
}

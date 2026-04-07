package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record RawDetection(
        Integer classId,
        String className,
        Double score,
        List<Double> bboxXyxy,
        Integer trackId,
        String colorHint,
        String notes
) {
}

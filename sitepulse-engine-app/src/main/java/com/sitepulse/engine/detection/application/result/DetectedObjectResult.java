package com.sitepulse.engine.detection.application.result;

import java.util.List;

public record DetectedObjectResult(
        Integer classId,
        String className,
        Double score,
        List<Double> bboxXyxy,
        Boolean inRoi
) {
}

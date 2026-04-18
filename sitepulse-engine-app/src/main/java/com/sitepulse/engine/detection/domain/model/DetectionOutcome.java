package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record DetectionOutcome(
        String modelVersion,
        String bucket,
        String key,
        Integer imageWidth,
        Integer imageHeight,
        Double inferenceMs,
        String weatherNote,
        List<DetectedObject> detections,
        List<String> warnings,
        boolean skipped
) {
}

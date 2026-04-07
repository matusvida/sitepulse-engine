package com.sitepulse.engine.detection.application.result;

import java.util.List;

public record DetectionOutcomeResult(
        String modelVersion,
        String bucket,
        String key,
        Integer imageWidth,
        Integer imageHeight,
        Double inferenceMs,
        List<DetectedObjectResult> detections,
        List<String> warnings,
        boolean skipped
) {
}

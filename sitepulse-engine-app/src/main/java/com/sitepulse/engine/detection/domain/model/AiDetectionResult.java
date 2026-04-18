package com.sitepulse.engine.detection.domain.model;

public record AiDetectionResult(
        DetectionInference inference,
        String rawResponse,
        String promptVersion,
        boolean roiIncluded
) {
}

package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record DetectionContext(
        Integer imageId,
        List<DetectionContextItem> detections,
        String previousDetectionResponse
) {
}

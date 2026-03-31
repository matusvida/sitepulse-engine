package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record DetectionInference(
        String modelVersion,
        Integer imageWidth,
        Integer imageHeight,
        Double inferenceMs,
        List<RawDetection> rawDetections
) {
}

package com.sitepulse.engine.detection.application.result;

public record DetectionHealthResult(String status, Boolean modelLoaded, String modelVersion) {
}

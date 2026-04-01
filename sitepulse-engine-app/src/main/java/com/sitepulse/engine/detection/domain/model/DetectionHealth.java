package com.sitepulse.engine.detection.domain.model;

public record DetectionHealth(String status, Boolean modelLoaded, String modelVersion) {
}

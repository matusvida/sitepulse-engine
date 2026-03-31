package com.sitepulse.engine.detection.domain.model;

public record DetectionTarget(String bucket, String key, Integer projectId, Integer imageId) {
}

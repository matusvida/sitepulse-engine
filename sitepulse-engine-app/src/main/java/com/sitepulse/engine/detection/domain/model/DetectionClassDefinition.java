package com.sitepulse.engine.detection.domain.model;

public record DetectionClassDefinition(
        Integer id,
        String className,
        String classGroup
) {
}

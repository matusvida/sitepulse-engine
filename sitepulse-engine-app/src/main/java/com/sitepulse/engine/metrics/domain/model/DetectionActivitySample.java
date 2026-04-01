package com.sitepulse.engine.metrics.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DetectionActivitySample(
        String className,
        Integer imageId,
        OffsetDateTime capturedAt,
        LocalDate capturedDate
) {
}

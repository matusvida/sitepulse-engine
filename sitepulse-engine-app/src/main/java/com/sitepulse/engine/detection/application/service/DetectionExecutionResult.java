package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionProvider;

public record DetectionExecutionResult(
        DetectionProvider provider,
        DetectionInference inference,
        Integer analysisRunId
) {
}

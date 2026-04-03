package com.sitepulse.engine.detection.infrastructure.external.openai;

import com.sitepulse.engine.detection.domain.model.DetectionInference;

public record OpenAiDetectionResult(
        DetectionInference inference,
        String rawResponse,
        String promptVersion
) {
}

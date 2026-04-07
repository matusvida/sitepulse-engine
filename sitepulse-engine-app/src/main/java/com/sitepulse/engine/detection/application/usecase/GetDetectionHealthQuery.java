package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.detection.application.result.DetectionHealthResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionService;
import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDetectionHealthQuery {

    private final DetectionExecutionService detectionExecutionService;

    public DetectionHealthResult get() {
        DetectionHealth health = detectionExecutionService.health();
        return new DetectionHealthResult(health.status(), health.modelLoaded(), health.modelVersion());
    }
}

package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.detection.application.result.DetectionHealthResult;
import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDetectionHealthQuery {

    private final DetectionGateway detectionGateway;

    public DetectionHealthResult get() {
        DetectionHealth health = detectionGateway.health();
        return new DetectionHealthResult(health.status(), health.modelLoaded(), health.modelVersion());
    }
}

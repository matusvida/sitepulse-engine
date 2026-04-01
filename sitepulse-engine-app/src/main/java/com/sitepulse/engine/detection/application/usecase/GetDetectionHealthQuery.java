package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDetectionHealthQuery {

    private final DetectionGateway detectionGateway;

    public DetectionHealth get() {
        return detectionGateway.health();
    }
}

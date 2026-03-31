package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionInference;

public interface DetectionGateway {

    DetectionHealth health();

    DetectionInference infer(byte[] imageBytes);
}

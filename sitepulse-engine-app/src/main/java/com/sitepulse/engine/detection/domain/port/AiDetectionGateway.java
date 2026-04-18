package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.AiDetectionResult;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectionContext;

public interface AiDetectionGateway {

    AiDetectionResult infer(byte[] imageBytes, DetectionContext context, CameraRoiSettings cameraSettings);

    String promptVersion();
}

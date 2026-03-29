package com.sitepulse.engine.detection.web;

import com.sitepulse.engine.detection.application.DetectionService;
import com.sitepulse.engine.http.detection.api.DetectionApi;
import com.sitepulse.engine.http.detection.dto.DetectRequest;
import com.sitepulse.engine.http.detection.dto.DetectResponse;
import com.sitepulse.engine.http.detection.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DetectionController implements DetectionApi {

    private final DetectionService detectionService;

    @Override
    public HealthResponse health() {
        return detectionService.yoloHealth();
    }

    @Override
    public DetectResponse detect(DetectRequest request) {
        return detectionService.detect(request);
    }
}

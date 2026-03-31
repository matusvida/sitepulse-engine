package com.sitepulse.engine.detection.infrastructure.external;

import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.RawDetection;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import com.sitepulse.engine.integration.yolo.YoloFeignClient;
import com.sitepulse.engine.integration.yolo.dto.YoloInferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class YoloDetectionGateway implements DetectionGateway {

    private final YoloFeignClient yoloFeignClient;

    @Override
    public DetectionHealth health() {
        var health = yoloFeignClient.health();
        return new DetectionHealth(health.getStatus(), health.getModelLoaded(), health.getModelVersion());
    }

    @Override
    public DetectionInference infer(byte[] imageBytes) {
        var response = yoloFeignClient.infer(new YoloInferRequest(Base64.getEncoder().encodeToString(imageBytes)));
        return new DetectionInference(
                response.getModelVersion(),
                response.getImageWidth(),
                response.getImageHeight(),
                response.getInferenceMs(),
                response.getRawDetections().stream()
                        .map(raw -> new RawDetection(raw.getClassId(), raw.getClassName(), raw.getScore(), raw.getBboxXyxy()))
                        .toList()
        );
    }
}

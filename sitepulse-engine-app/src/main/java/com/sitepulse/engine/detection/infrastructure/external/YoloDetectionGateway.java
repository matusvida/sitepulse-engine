package com.sitepulse.engine.detection.infrastructure.external;

import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.RawDetection;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import com.sitepulse.engine.detection.application.service.DetectionClassCatalog;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import com.sitepulse.engine.detection.infrastructure.external.yolo.YoloFeignClient;
import com.sitepulse.engine.detection.infrastructure.external.yolo.dto.YoloInferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class YoloDetectionGateway implements DetectionGateway {

    private final YoloFeignClient yoloFeignClient;
    private final DetectionClassCatalog detectionClassCatalog;

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
                        .map(raw -> {
                            DetectionClassEntity resolved = resolveDetectionClass(raw.getClassName());
                            return new RawDetection(
                                    resolved.getId(),
                                    resolved.getClassName(),
                                    raw.getScore(),
                                    raw.getBboxXyxy(),
                                    null,
                                    null,
                                    null
                            );
                        })
                        .toList()
        );
    }

    private DetectionClassEntity resolveDetectionClass(String className) {
        if (className == null || className.isBlank()) {
            return detectionClassCatalog.resolveByNameOrDefault("other_equipment", "other_equipment");
        }
        return detectionClassCatalog.findByName(className)
                .or(() -> detectionClassCatalog.findByName(className.toLowerCase(Locale.ROOT)))
                .orElseGet(() -> detectionClassCatalog.resolveByNameOrDefault("other_equipment", "other_equipment"));
    }
}

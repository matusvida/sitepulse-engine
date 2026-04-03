package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionProvider;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import com.sitepulse.engine.detection.infrastructure.external.openai.OpenAiDetectionGateway;
import com.sitepulse.engine.detection.infrastructure.external.openai.OpenAiDetectionResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionExecutionService {

    private final SitePulseProperties properties;
    private final DetectionGateway yoloDetectionGateway;
    private final OpenAiDetectionGateway openAiDetectionGateway;
    private final DetectionContextService detectionContextService;
    private final DetectionAnalysisRunService detectionAnalysisRunService;
    private final CameraLookup cameraLookup;

    public DetectionExecutionResult execute(DetectionImage image, byte[] imageBytes) {
        DetectionProvider provider = DetectionProvider.from(properties.detectionProvider());
        Integer previousImageId = detectionContextService.findPreviousContext(image).map(DetectionContext::imageId).orElse(null);
        if (provider == DetectionProvider.YOLO) {
            return runYolo(image, imageBytes, previousImageId);
        }
        return runOpenAiWithFallback(image, imageBytes, previousImageId);
    }

    public DetectionHealth health() {
        DetectionProvider provider = DetectionProvider.from(properties.detectionProvider());
        if (provider == DetectionProvider.YOLO) {
            return yoloDetectionGateway.health();
        }
        boolean loaded = properties.openaiApiKey() != null && !properties.openaiApiKey().isBlank();
        return new DetectionHealth(loaded ? "ok" : "down", loaded, properties.openaiModel());
    }

    private DetectionExecutionResult runOpenAiWithFallback(DetectionImage image, byte[] imageBytes, Integer previousImageId) {
        Optional<DetectionContext> context = detectionContextService.findPreviousContext(image);
        Integer cameraWidth = image.getProjectId() == null ? null : cameraLookup.findImageWidth(image.getProjectId(), image.getKey());
        Integer cameraHeight = image.getProjectId() == null ? null : cameraLookup.findImageHeight(image.getProjectId(), image.getKey());
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                OpenAiDetectionResult result = openAiDetectionGateway.infer(imageBytes, context.orElse(null), cameraWidth, cameraHeight);
                Integer runId = detectionAnalysisRunService.recordRun(
                        image.getId(),
                        previousImageId,
                        DetectionProvider.OPENAI.name().toLowerCase(),
                        result.inference().modelVersion(),
                        result.promptVersion(),
                        attempt - 1,
                        "success",
                        result.inference().inferenceMs(),
                        null,
                        result.rawResponse()
                );
                return new DetectionExecutionResult(DetectionProvider.OPENAI, result.inference(), runId);
            } catch (RuntimeException ex) {
                detectionAnalysisRunService.recordRun(
                        image.getId(),
                        previousImageId,
                        DetectionProvider.OPENAI.name().toLowerCase(),
                        properties.openaiModel(),
                        openAiDetectionGateway.promptVersion(),
                        attempt - 1,
                        "failed",
                        null,
                        ex.getMessage(),
                        null
                );
                log.warn("OpenAI detection failed (attempt {}/3) for imageId={} reason={}", attempt, image.getId(), ex.getMessage());
            }
        }
        return runYolo(image, imageBytes, previousImageId);
    }

    private DetectionExecutionResult runYolo(DetectionImage image, byte[] imageBytes, Integer previousImageId) {
        DetectionInference inference = yoloDetectionGateway.infer(imageBytes);
        Integer runId = detectionAnalysisRunService.recordRun(
                image.getId(),
                previousImageId,
                DetectionProvider.YOLO.name().toLowerCase(),
                inference.modelVersion(),
                null,
                0,
                "success",
                inference.inferenceMs(),
                null,
                null
        );
        return new DetectionExecutionResult(DetectionProvider.YOLO, inference, runId);
    }
}

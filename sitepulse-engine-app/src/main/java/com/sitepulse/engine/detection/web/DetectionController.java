package com.sitepulse.engine.detection.web;

import com.sitepulse.engine.detection.application.command.RunOnDemandDetectionCommand;
import com.sitepulse.engine.detection.application.usecase.GetDetectionHealthQuery;
import com.sitepulse.engine.detection.application.usecase.RunOnDemandDetectionUseCase;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionHealth;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.http.detection.api.DetectionApi;
import com.sitepulse.engine.http.detection.dto.DetectRequest;
import com.sitepulse.engine.http.detection.dto.DetectResponse;
import com.sitepulse.engine.http.detection.dto.DetectionView;
import com.sitepulse.engine.http.detection.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DetectionController implements DetectionApi {

    private final GetDetectionHealthQuery getDetectionHealthQuery;
    private final RunOnDemandDetectionUseCase runOnDemandDetectionUseCase;

    @Override
    public HealthResponse health() {
        DetectionHealth detectionHealth = getDetectionHealthQuery.get();
        return new HealthResponse(detectionHealth.status(), detectionHealth.modelLoaded(), detectionHealth.modelVersion());
    }

    @Override
    public DetectResponse detect(DetectRequest request) {
        DetectionOutcome outcome = runOnDemandDetectionUseCase.run(
                new RunOnDemandDetectionCommand(request.getBucket(), request.getKey(), request.getS3Url())
        );
        return new DetectResponse(
                outcome.modelVersion(),
                outcome.bucket(),
                outcome.key(),
                outcome.imageWidth(),
                outcome.imageHeight(),
                outcome.inferenceMs(),
                outcome.detections().stream().map(this::toView).toList(),
                outcome.warnings()
        );
    }

    private DetectionView toView(DetectedObject detectedObject) {
        return new DetectionView(
                detectedObject.classId(),
                detectedObject.className(),
                detectedObject.score(),
                detectedObject.bboxXyxy(),
                detectedObject.inRoi()
        );
    }
}

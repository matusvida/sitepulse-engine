package com.sitepulse.engine.detection.web;

import com.sitepulse.engine.detection.application.command.RunOnDemandDetectionCommand;
import com.sitepulse.engine.detection.application.result.DetectedObjectResult;
import com.sitepulse.engine.detection.application.result.DetectionHealthResult;
import com.sitepulse.engine.detection.application.result.DetectionOutcomeResult;
import com.sitepulse.engine.detection.application.usecase.GetDetectionHealthQuery;
import com.sitepulse.engine.detection.application.usecase.RunOnDemandDetectionUseCase;
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
        DetectionHealthResult health = getDetectionHealthQuery.get();
        return new HealthResponse(health.status(), health.modelLoaded(), health.modelVersion());
    }

    @Override
    public DetectResponse detect(DetectRequest request) {
        DetectionOutcomeResult outcome = runOnDemandDetectionUseCase.run(
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

    private DetectionView toView(DetectedObjectResult d) {
        return new DetectionView(d.classId(), d.className(), d.score(), d.bboxXyxy(), d.inRoi(), d.trackId(), d.colorHint(), d.notes());
    }
}

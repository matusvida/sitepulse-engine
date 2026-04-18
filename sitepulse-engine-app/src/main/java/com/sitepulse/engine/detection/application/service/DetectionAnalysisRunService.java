package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.application.port.DetectionAnalysisRunStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAnalysisRunService {

    private final DetectionAnalysisRunStore detectionAnalysisRunStore;

    public Integer startRun(
            Integer imageId,
            Integer previousImageId,
            String provider,
            String modelVersion,
            String promptVersion,
            Integer retryCount
    ) {
        return detectionAnalysisRunStore.startRun(imageId, previousImageId, provider, modelVersion, promptVersion, retryCount);
    }

    public void completeSuccess(Integer runId, Double latencyMs, String rawResponse) {
        detectionAnalysisRunStore.completeSuccess(runId, latencyMs, rawResponse);
    }

    public void completeFailure(Integer runId, String error) {
        detectionAnalysisRunStore.completeFailure(runId, error);
    }
}

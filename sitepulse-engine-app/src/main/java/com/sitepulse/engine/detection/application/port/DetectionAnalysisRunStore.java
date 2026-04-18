package com.sitepulse.engine.detection.application.port;

public interface DetectionAnalysisRunStore {

    Integer startRun(
            Integer imageId,
            Integer previousImageId,
            String provider,
            String modelVersion,
            String promptVersion,
            Integer retryCount
    );

    void completeSuccess(Integer runId, Double latencyMs, String rawResponse);

    void completeFailure(Integer runId, String error);
}

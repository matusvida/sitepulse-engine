package com.sitepulse.engine.detection.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.detection.application.port.DetectionAnalysisRunStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class DetectionAnalysisRunStoreAdapter implements DetectionAnalysisRunStore {

    private final DetectionAnalysisRunRepository detectionAnalysisRunRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Integer startRun(
            Integer imageId,
            Integer previousImageId,
            String provider,
            String modelVersion,
            String promptVersion,
            Integer retryCount
    ) {
        DetectionAnalysisRunEntity run = DetectionAnalysisRunEntity.builder()
                .imageId(imageId)
                .previousImageId(previousImageId)
                .provider(provider)
                .modelVersion(modelVersion)
                .promptVersion(promptVersion)
                .retryCount(retryCount)
                .status("processing")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return detectionAnalysisRunRepository.save(run).getId();
    }

    @Override
    @Transactional
    public void completeSuccess(Integer runId, Double latencyMs, String rawResponse) {
        DetectionAnalysisRunEntity run = detectionAnalysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Detection analysis run not found: " + runId));
        run.setStatus("success");
        run.setLatencyMs(latencyMs);
        run.setError(null);
        run.setRawResponse(toJsonNode(rawResponse));
        detectionAnalysisRunRepository.save(run);
    }

    @Override
    @Transactional
    public void completeFailure(Integer runId, String error) {
        DetectionAnalysisRunEntity run = detectionAnalysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Detection analysis run not found: " + runId));
        run.setStatus("failed");
        run.setError(error);
        detectionAnalysisRunRepository.save(run);
    }

    private JsonNode toJsonNode(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawResponse);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse analysis run raw response as JSON", ex);
        }
    }
}

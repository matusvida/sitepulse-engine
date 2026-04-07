package com.sitepulse.engine.detection.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionAnalysisRunEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionAnalysisRunRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAnalysisRunService {

    private final DetectionAnalysisRunRepository detectionAnalysisRunRepository;
    private final ObjectMapper objectMapper;

    public Integer recordRun(
            Integer imageId,
            Integer previousImageId,
            String provider,
            String modelVersion,
            String promptVersion,
            Integer retryCount,
            String status,
            Double latencyMs,
            String error,
            String rawResponse
    ) {
        DetectionAnalysisRunEntity run = DetectionAnalysisRunEntity.builder()
                .imageId(imageId)
                .previousImageId(previousImageId)
                .provider(provider)
                .modelVersion(modelVersion)
                .promptVersion(promptVersion)
                .retryCount(retryCount)
                .status(status)
                .latencyMs(latencyMs)
                .error(error)
                .rawResponse(toJsonNode(rawResponse))
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return detectionAnalysisRunRepository.save(run).getId();
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

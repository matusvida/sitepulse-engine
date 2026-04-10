package com.sitepulse.engine.detection.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionContextItem;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.ImageStatus;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionAnalysisRunRepository;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionRepository;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionContextService {

    private final ImageRepository imageRepository;
    private final DetectionRepository detectionRepository;
    private final JsonUtils jsonUtils;
    private final DetectionClassCatalog detectionClassCatalog;
    private final DetectionAnalysisRunRepository detectionAnalysisRunRepository;
    private final ObjectMapper objectMapper;

    public Optional<DetectionContext> findPreviousContext(DetectionImage image) {
        if (image.getProjectId() == null) {
            return Optional.empty();
        }
        Optional<ImageEntity> previous = findPreviousImage(image);
        if (previous.isEmpty()) {
            return Optional.empty();
        }
        List<DetectionContextItem> items = detectionRepository.findByImageId(previous.get().getId()).stream()
                .map(this::toContextItem)
                .toList();
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DetectionContext(
                previous.get().getId(),
                items,
                findPreviousDetectionResponse(previous.get().getId())
        ));
    }

    private Optional<ImageEntity> findPreviousImage(DetectionImage image) {
        Integer projectId = image.getProjectId();
        Integer cameraId = image.getCameraId();
        OffsetDateTime capturedAt = image.getCapturedAt();
        Integer imageId = image.getId();
        if (cameraId != null) {
            if (capturedAt != null) {
                return imageRepository.findPreviousByCameraCapturedAt(projectId, cameraId, ImageStatus.DONE.name(), capturedAt);
            }
            if (imageId != null) {
                return imageRepository.findPreviousByCameraId(projectId, cameraId, ImageStatus.DONE.name(), imageId);
            }
        }
        if (capturedAt != null) {
            return imageRepository.findPreviousByProjectCapturedAt(projectId, ImageStatus.DONE.name(), capturedAt);
        }
        if (imageId != null) {
            return imageRepository.findPreviousByProjectId(projectId, ImageStatus.DONE.name(), imageId);
        }
        return Optional.empty();
    }

    private DetectionContextItem toContextItem(DetectionEntity detectionEntity) {
        String className = detectionClassCatalog.findById(detectionEntity.getClassId())
                .map(DetectionClassEntity::getClassName)
                .orElse("unknown");
        return new DetectionContextItem(
                detectionEntity.getTrackId(),
                detectionEntity.getClassId(),
                className,
                jsonUtils.readDoubleList(detectionEntity.getBboxXyxy()),
                detectionEntity.getColorHint()
        );
    }

    private String findPreviousDetectionResponse(Integer imageId) {
        return detectionAnalysisRunRepository.findTopByImageIdOrderByIdDesc(imageId)
                .map(run -> serialize(run.getRawResponse()))
                .orElse(null);
    }

    private String serialize(JsonNode rawResponse) {
        if (rawResponse == null || rawResponse.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rawResponse);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize previous detection response", ex);
        }
    }
}

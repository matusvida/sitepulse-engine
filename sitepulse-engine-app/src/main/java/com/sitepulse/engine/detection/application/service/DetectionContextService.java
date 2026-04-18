package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionContextItem;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionContextService {

    private final ProcessedImageReadModel processedImageReadModel;
    public Optional<DetectionContext> findPreviousContext(DetectionImage image) {
        if (image.getProjectId() == null) {
            return Optional.empty();
        }
        Optional<StoredImage> previous = processedImageReadModel.findPreviousDoneImage(
                image.getProjectId(),
                image.getCameraId(),
                image.getCapturedAt(),
                image.getId());
        if (previous.isEmpty()) {
            return Optional.empty();
        }
        List<DetectionContextItem> items = processedImageReadModel.findDetections(previous.get().getId()).stream()
                .map(this::toContextItem)
                .toList();
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DetectionContext(
                previous.get().getId(),
                items
        ));
    }

    private DetectionContextItem toContextItem(DetectedObject detection) {
        return new DetectionContextItem(
                detection.trackId(),
                detection.classId(),
                detection.className(),
                detection.bboxXyxy(),
                detection.colorHint()
        );
    }
}

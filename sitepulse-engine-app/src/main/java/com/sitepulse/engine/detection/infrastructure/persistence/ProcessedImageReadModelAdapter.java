package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionClassDefinition;
import com.sitepulse.engine.detection.domain.enums.ImageStatus;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.DetectionClassCatalog;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProcessedImageReadModelAdapter implements ProcessedImageReadModel {

    private final ImageRepository imageRepository;
    private final DetectionRepository detectionRepository;
    private final JsonUtils jsonUtils;
    private final DetectionClassCatalog detectionClassCatalog;

    @Override
    public List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId) {
        return imageRepository.findCapturedAtValuesByProjectIdAndStatusIn(projectId, List.of(ImageStatus.NEW, ImageStatus.DONE));
    }

    @Override
    public List<StoredImage> findRepresentativeSnapshots(Integer projectId) {
        return imageRepository.findRepresentativeSnapshots(projectId).stream()
                .map(this::toStoredImage)
                .toList();
    }

    @Override
    public Optional<StoredImage> findClosestSnapshot(Integer projectId, OffsetDateTime dayStart, OffsetDateTime dayEnd, OffsetDateTime midday) {
        return imageRepository.findClosestSnapshot(projectId, dayStart, dayEnd, midday).map(this::toStoredImage);
    }

    @Override
    public Optional<StoredImage> findPreviousDoneImage(Integer projectId, Integer cameraId, OffsetDateTime capturedAt, Integer imageId) {
        Optional<ImageEntity> previous;
        if (cameraId != null) {
            if (capturedAt != null) {
                previous = imageRepository.findPreviousByCameraCapturedAt(projectId, cameraId, ImageStatus.DONE.name(), capturedAt);
            } else if (imageId != null) {
                previous = imageRepository.findPreviousByCameraId(projectId, cameraId, ImageStatus.DONE.name(), imageId);
            } else {
                previous = Optional.empty();
            }
        } else if (capturedAt != null) {
            previous = imageRepository.findPreviousByProjectCapturedAt(projectId, ImageStatus.DONE.name(), capturedAt);
        } else if (imageId != null) {
            previous = imageRepository.findPreviousByProjectId(projectId, ImageStatus.DONE.name(), imageId);
        } else {
            previous = Optional.empty();
        }
        return previous.map(this::toStoredImage);
    }

    @Override
    public List<StoredImage> findDoneInRange(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
        return imageRepository.findDoneInRange(projectId, from, to).stream().map(this::toStoredImage).toList();
    }

    @Override
    public List<StoredImage> findProcessedByProject(Integer projectId) {
        return imageRepository.findProcessedByProject(projectId).stream().map(this::toStoredImage).toList();
    }

    @Override
    public List<DetectedObject> findDetections(Integer imageId) {
        return detectionRepository.findByImageId(imageId).stream()
                .map(this::toDetectedObject)
                .toList();
    }

    private StoredImage toStoredImage(ImageEntity imageEntity) {
        return new StoredImage(
                imageEntity.getId(),
                imageEntity.getBucket(),
                imageEntity.getKey(),
                imageEntity.getCapturedAt(),
                imageEntity.getWeatherNote(),
                imageEntity.getEvidenceActivityScore(),
                imageEntity.getEvidenceChangeScore(),
                imageEntity.getEvidenceQualityScore(),
                imageEntity.getEvidenceOverallScore(),
                imageEntity.getEvidenceSummary()
        );
    }

    private DetectedObject toDetectedObject(DetectionEntity detectionEntity) {
        String className = detectionClassCatalog.findById(detectionEntity.getClassId())
                .map(DetectionClassDefinition::className)
                .orElse("unknown");
        return new DetectedObject(
                detectionEntity.getClassId(),
                className,
                detectionEntity.getScore(),
                jsonUtils.readDoubleList(detectionEntity.getBboxXyxy()),
                detectionEntity.getInRoi(),
                detectionEntity.getTrackId(),
                detectionEntity.getColorHint(),
                detectionEntity.getNotes()
        );
    }
}

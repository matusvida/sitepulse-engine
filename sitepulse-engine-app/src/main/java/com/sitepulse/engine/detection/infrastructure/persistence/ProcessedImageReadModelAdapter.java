package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.application.service.DetectionClassCatalog;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.detection.domain.model.ImageStatus;
import com.sitepulse.engine.detection.domain.model.StoredImage;
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
                imageEntity.getCapturedAt()
        );
    }

    private DetectedObject toDetectedObject(DetectionEntity detectionEntity) {
        String className = detectionClassCatalog.findById(detectionEntity.getClassId())
                .map(DetectionClassEntity::getClassName)
                .orElse("unknown");
        return new DetectedObject(
                detectionEntity.getClassId(),
                className,
                detectionEntity.getScore(),
                jsonUtils.readDoubleList(detectionEntity.getBboxXyxy()),
                detectionEntity.getInRoi() == null ? null : Boolean.valueOf(detectionEntity.getInRoi()),
                detectionEntity.getTrackId(),
                detectionEntity.getColorHint(),
                detectionEntity.getNotes()
        );
    }
}

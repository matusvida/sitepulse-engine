package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.port.DetectionRecordRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class DetectionRecordRepositoryAdapter implements DetectionRecordRepository {

    private final DetectionRepository detectionRepository;
    private final JsonUtils jsonUtils;

    @Override
    @Transactional
    public void replaceDetections(Integer imageId, Integer projectId, String modelVersion, Integer analysisRunId, List<DetectedObject> detections) {
        detectionRepository.deleteByImageId(imageId);
        for (DetectedObject detection : detections) {
            detectionRepository.save(DetectionEntity.builder()
                    .imageId(imageId)
                    .projectId(projectId)
                    .modelVersion(modelVersion)
                    .classId(detection.classId())
                    .score(detection.score())
                    .bboxXyxy(jsonUtils.write(detection.bboxXyxy()))
                    .inRoi(detection.inRoi())
                    .trackId(detection.trackId())
                    .analysisRunId(analysisRunId)
                    .colorHint(detection.colorHint())
                    .notes(detection.notes())
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        }
    }

    @Override
    public List<DetectedObject> findDetections(Integer imageId) {
        return detectionRepository.findByImageId(imageId).stream()
                .map(this::toDomain)
                .toList();
    }

    private DetectedObject toDomain(DetectionEntity entity) {
        return new DetectedObject(
                entity.getClassId(),
                resolveClassName(entity).orElse("unknown"),
                entity.getScore(),
                jsonUtils.readDoubleList(entity.getBboxXyxy()),
                entity.getInRoi(),
                entity.getTrackId(),
                entity.getColorHint(),
                entity.getNotes()
        );
    }

    private Optional<String> resolveClassName(DetectionEntity entity) {
        return Optional.ofNullable(entity.getClassId()).flatMap(detectionRepository::findClassNameByClassId);
    }
}

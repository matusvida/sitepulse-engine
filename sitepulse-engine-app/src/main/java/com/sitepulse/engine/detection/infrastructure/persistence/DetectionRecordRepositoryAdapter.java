package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.domain.DetectionEntity;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.port.DetectionRecordRepository;
import com.sitepulse.engine.detection.persistence.DetectionRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
    public void replaceDetections(Integer imageId, Integer projectId, String modelVersion, List<DetectedObject> detections) {
        detectionRepository.deleteByImageId(imageId);
        for (DetectedObject detection : detections) {
            detectionRepository.save(DetectionEntity.builder()
                    .imageId(imageId)
                    .projectId(projectId)
                    .modelVersion(modelVersion)
                    .classId(detection.classId())
                    .className(detection.className())
                    .score(detection.score())
                    .bboxXyxy(jsonUtils.write(detection.bboxXyxy()))
                    .inRoi(detection.inRoi() == null ? null : detection.inRoi().toString())
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        }
    }
}

package com.sitepulse.engine.sync.application;

import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.domain.ImageStatus;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.project.application.ProjectService;
import com.sitepulse.engine.project.domain.CameraEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncImagePersistenceService {

    private final ImageRepository imageRepository;
    private final ProjectService projectService;

    public boolean exists(String bucket, String key) {
        return imageRepository.existsByBucketAndKey(bucket, key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSyncedImage(String bucket, String key, Integer projectId, OffsetDateTime capturedAt) {
        CameraEntity camera = projectService.findCameraByKey(projectId, key);
        imageRepository.save(ImageEntity.builder()
                .bucket(bucket)
                .key(key)
                .status(ImageStatus.NEW)
                .projectId(projectId)
                .cameraId(camera == null ? null : camera.getId())
                .capturedAt(capturedAt)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }
}

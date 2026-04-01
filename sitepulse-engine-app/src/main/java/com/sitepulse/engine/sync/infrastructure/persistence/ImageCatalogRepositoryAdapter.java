package com.sitepulse.engine.sync.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.ImageStatus;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.project.infrastructure.persistence.CameraEntity;
import com.sitepulse.engine.project.infrastructure.persistence.CameraRepository;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import com.sitepulse.engine.sync.domain.port.ImageCatalogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ImageCatalogRepositoryAdapter implements ImageCatalogRepository {

    private final ImageRepository imageRepository;
    private final CameraRepository cameraRepository;

    @Override
    public boolean exists(String bucket, String key) {
        return imageRepository.existsByBucketAndKey(bucket, key);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveImportedImage(ImageImport imageImport) {
        CameraEntity camera = resolveCamera(imageImport.projectId(), imageImport.key());
        imageRepository.save(ImageEntity.builder()
                .bucket(imageImport.bucket())
                .key(imageImport.key())
                .status(ImageStatus.NEW)
                .projectId(imageImport.projectId())
                .cameraId(camera == null ? null : camera.getId())
                .capturedAt(imageImport.capturedAt())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }

    private CameraEntity resolveCamera(Integer projectId, String key) {
        return cameraRepository.findByProjectIdAndKeyPrefixIsNotNullOrderByKeyPrefixDesc(projectId).stream()
                .filter(camera -> key.startsWith(camera.getKeyPrefix()))
                .max(Comparator.comparingInt(camera -> camera.getKeyPrefix().length()))
                .orElse(null);
    }
}

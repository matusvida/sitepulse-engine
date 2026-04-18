package com.sitepulse.engine.snapshot.infrastructure.persistence;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.snapshot.application.port.CameraSnapshotProfileStore;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CameraSnapshotProfileStoreAdapter implements CameraSnapshotProfileStore {

    private final CameraSnapshotProfileRepository repository;

    @Override
    public java.util.Optional<CameraSnapshotProfile> findByCameraId(Integer cameraId) {
        return repository.findByCameraId(cameraId).map(this::toProfile);
    }

    @Override
    public CameraSnapshotProfile save(CameraSnapshotProfile profile) {
        CameraSnapshotProfileEntity entity = repository.findByCameraId(profile.cameraId())
                .orElseGet(CameraSnapshotProfileEntity::new);
        entity.setCameraId(profile.cameraId());
        entity.setTargetWidth(profile.targetWidth());
        entity.setTargetQuality(profile.targetQuality());
        entity.setTargetFormat(profile.targetFormat().getCanonicalExtension());
        entity.setFreezeTime(profile.freezeTime());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
        }
        entity.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
        return toProfile(repository.save(entity));
    }

    private CameraSnapshotProfile toProfile(CameraSnapshotProfileEntity entity) {
        return new CameraSnapshotProfile(
                entity.getCameraId(),
                entity.getTargetWidth(),
                entity.getTargetQuality(),
                ImageFormat.fromConfiguredFormat(entity.getTargetFormat(), ImageFormat.WEBP),
                entity.getFreezeTime()
        );
    }
}

package com.sitepulse.engine.snapshot.infrastructure.persistence;

import com.sitepulse.engine.snapshot.application.port.CameraDailySnapshotStore;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotAsset;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CameraDailySnapshotStoreAdapter implements CameraDailySnapshotStore {

    private final CameraDailySnapshotRepository repository;

    @Override
    public Optional<CameraSnapshotAsset> findByCameraIdAndSnapshotDate(Integer cameraId, LocalDate snapshotDate) {
        return repository.findByCameraIdAndSnapshotDate(cameraId, snapshotDate).map(this::toAsset);
    }

    @Override
    public CameraSnapshotAsset save(CameraSnapshotAsset snapshotAsset) {
        CameraDailySnapshotEntity entity = repository.findByCameraIdAndSnapshotDate(snapshotAsset.cameraId(), snapshotAsset.snapshotDate())
                .orElseGet(CameraDailySnapshotEntity::new);
        entity.setCameraId(snapshotAsset.cameraId());
        entity.setSnapshotDate(snapshotAsset.snapshotDate());
        entity.setSourceImageId(snapshotAsset.sourceImageId());
        entity.setBucket(snapshotAsset.bucket());
        entity.setKey(snapshotAsset.key());
        entity.setMediaType(snapshotAsset.mediaType());
        entity.setFrozen(snapshotAsset.frozen());
        entity.setGeneratedAt(snapshotAsset.generatedAt());
        entity.setUpdatedAt(snapshotAsset.generatedAt());
        return toAsset(repository.save(entity));
    }

    private CameraSnapshotAsset toAsset(CameraDailySnapshotEntity entity) {
        return new CameraSnapshotAsset(
                entity.getCameraId(),
                entity.getSnapshotDate(),
                entity.getSourceImageId(),
                entity.getBucket(),
                entity.getKey(),
                entity.getMediaType(),
                entity.isFrozen(),
                entity.getUpdatedAt()
        );
    }
}

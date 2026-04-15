package com.sitepulse.engine.snapshot.application.service;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.infrastructure.persistence.CameraSnapshotProfileEntity;
import com.sitepulse.engine.snapshot.infrastructure.persistence.CameraSnapshotProfileRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CameraSnapshotProfileService {

    private final CameraSnapshotProfileRepository repository;
    private final SitePulseProperties properties;
    private final Clock clock;

    @Transactional
    public CameraSnapshotProfile getOrCreate(Integer cameraId) {
        return repository.findByCameraId(cameraId)
                .map(this::toResult)
                .orElseGet(() -> toResult(repository.save(defaultEntity(cameraId))));
    }

    private CameraSnapshotProfileEntity defaultEntity(Integer cameraId) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        SitePulseProperties.ImageWebSnapshotsProperties defaults = properties.imageWebSnapshots();
        return CameraSnapshotProfileEntity.builder()
                .cameraId(cameraId)
                .targetWidth(defaults.targetWidth())
                .targetQuality(defaults.targetQuality())
                .targetFormat(defaults.targetFormat().getCanonicalExtension())
                .freezeTime(defaults.freezeTime())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private CameraSnapshotProfile toResult(CameraSnapshotProfileEntity entity) {
        return new CameraSnapshotProfile(
                entity.getCameraId(),
                entity.getTargetWidth(),
                entity.getTargetQuality(),
                ImageFormat.fromConfiguredFormat(entity.getTargetFormat(), ImageFormat.WEBP),
                entity.getFreezeTime()
        );
    }
}

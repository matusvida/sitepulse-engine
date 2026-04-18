package com.sitepulse.engine.snapshot.application.service;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.snapshot.application.port.CameraSnapshotProfileStore;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CameraSnapshotProfileService {

    private final CameraSnapshotProfileStore store;
    private final SitePulseProperties properties;

    @Transactional
    public CameraSnapshotProfile getOrCreate(Integer cameraId) {
        return store.findByCameraId(cameraId)
                .orElseGet(() -> store.save(defaultProfile(cameraId)));
    }

    private CameraSnapshotProfile defaultProfile(Integer cameraId) {
        SitePulseProperties.ImageWebSnapshotsProperties defaults = properties.imageWebSnapshots();
        return new CameraSnapshotProfile(
                cameraId,
                defaults.targetWidth(),
                defaults.targetQuality(),
                ImageFormat.fromConfiguredFormat(defaults.targetFormat().getCanonicalExtension(), ImageFormat.WEBP),
                defaults.freezeTime()
        );
    }
}

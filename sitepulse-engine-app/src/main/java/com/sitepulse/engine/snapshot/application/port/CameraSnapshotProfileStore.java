package com.sitepulse.engine.snapshot.application.port;

import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import java.util.Optional;

public interface CameraSnapshotProfileStore {

    Optional<CameraSnapshotProfile> findByCameraId(Integer cameraId);

    CameraSnapshotProfile save(CameraSnapshotProfile profile);
}

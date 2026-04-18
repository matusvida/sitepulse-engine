package com.sitepulse.engine.snapshot.application.port;

import com.sitepulse.engine.snapshot.application.result.CameraSnapshotAsset;
import java.time.LocalDate;
import java.util.Optional;

public interface CameraDailySnapshotStore {

    Optional<CameraSnapshotAsset> findByCameraIdAndSnapshotDate(Integer cameraId, LocalDate snapshotDate);

    CameraSnapshotAsset save(CameraSnapshotAsset snapshotAsset);
}

package com.sitepulse.engine.snapshot.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraSnapshotProfileRepository extends JpaRepository<CameraSnapshotProfileEntity, Long> {

    Optional<CameraSnapshotProfileEntity> findByCameraId(Integer cameraId);
}

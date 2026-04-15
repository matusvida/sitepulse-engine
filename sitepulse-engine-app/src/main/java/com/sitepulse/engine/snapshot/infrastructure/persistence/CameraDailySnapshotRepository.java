package com.sitepulse.engine.snapshot.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraDailySnapshotRepository extends JpaRepository<CameraDailySnapshotEntity, Long> {

    Optional<CameraDailySnapshotEntity> findByCameraIdAndSnapshotDate(Integer cameraId, LocalDate snapshotDate);

    List<CameraDailySnapshotEntity> findByCameraIdOrderBySnapshotDateAsc(Integer cameraId);

    List<CameraDailySnapshotEntity> findByCameraIdAndSnapshotDateInOrderBySnapshotDateAsc(Integer cameraId, List<LocalDate> snapshotDates);
}

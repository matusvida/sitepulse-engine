package com.sitepulse.engine.detection.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionTrackRepository extends JpaRepository<DetectionTrackEntity, Integer> {

    List<DetectionTrackEntity> findTop20ByProjectIdAndCameraIdAndActiveTrueOrderByLastSeenImageIdDesc(Integer projectId, Integer cameraId);

    List<DetectionTrackEntity> findByProjectIdAndCameraIdAndActiveTrue(Integer projectId, Integer cameraId);

    Optional<DetectionTrackEntity> findByIdAndProjectIdAndCameraId(Integer id, Integer projectId, Integer cameraId);
}

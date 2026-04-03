package com.sitepulse.engine.detection.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionAnalysisRunRepository extends JpaRepository<DetectionAnalysisRunEntity, Integer> {

    Optional<DetectionAnalysisRunEntity> findTopByImageIdOrderByIdDesc(Integer imageId);
}

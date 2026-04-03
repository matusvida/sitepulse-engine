package com.sitepulse.engine.detection.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionClassRepository extends JpaRepository<DetectionClassEntity, Integer> {

    Optional<DetectionClassEntity> findByClassNameIgnoreCase(String className);
}

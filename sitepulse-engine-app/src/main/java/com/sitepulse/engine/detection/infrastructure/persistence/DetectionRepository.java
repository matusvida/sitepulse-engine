package com.sitepulse.engine.detection.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionRepository extends JpaRepository<DetectionEntity, Integer> {

    List<DetectionEntity> findByImageId(Integer imageId);

    void deleteByImageId(Integer imageId);
}

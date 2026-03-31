package com.sitepulse.engine.detection.persistence;

import com.sitepulse.engine.detection.domain.DetectionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionRepository extends JpaRepository<DetectionEntity, Integer> {

    List<DetectionEntity> findByImageId(Integer imageId);

    void deleteByImageId(Integer imageId);
}

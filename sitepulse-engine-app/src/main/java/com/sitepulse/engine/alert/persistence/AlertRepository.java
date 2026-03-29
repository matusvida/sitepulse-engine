package com.sitepulse.engine.alert.persistence;

import com.sitepulse.engine.alert.domain.AlertEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, Integer> {

    boolean existsByProjectIdAndTypeAndStatus(Integer projectId, String type, String status);

    List<AlertEntity> findByProjectIdOrderByCreatedAtDesc(Integer projectId);

    List<AlertEntity> findByProjectIdAndTypeAndSeverityAndStatusOrderByCreatedAtDesc(
            Integer projectId,
            String type,
            String severity,
            String status
    );

    List<AlertEntity> findByProjectIdAndTypeAndStatus(Integer projectId, String type, String status);

    Optional<AlertEntity> findByIdAndProjectId(Integer id, Integer projectId);
}

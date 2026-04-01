package com.sitepulse.engine.project.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraRepository extends JpaRepository<CameraEntity, Integer> {

    List<CameraEntity> findByProjectIdOrderById(Integer projectId);

    List<CameraEntity> findByProjectIdAndKeyPrefixIsNotNullOrderByKeyPrefixDesc(Integer projectId);

    Optional<CameraEntity> findByIdAndProjectId(Integer id, Integer projectId);
}

package com.sitepulse.engine.plan.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstructionPlanRepository extends JpaRepository<ConstructionPlanEntity, Integer> {

    Optional<ConstructionPlanEntity> findTopByProjectIdOrderByCreatedAtDesc(Integer projectId);

    Optional<ConstructionPlanEntity> findTopByProjectIdAndStatusOrderByCreatedAtDesc(Integer projectId, String status);

    List<ConstructionPlanEntity> findByStatus(String status);
}

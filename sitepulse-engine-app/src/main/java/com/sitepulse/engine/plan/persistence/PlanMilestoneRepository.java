package com.sitepulse.engine.plan.persistence;

import com.sitepulse.engine.plan.domain.PlanMilestoneEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanMilestoneRepository extends JpaRepository<PlanMilestoneEntity, Integer> {

    List<PlanMilestoneEntity> findByPlanIdOrderByWeekNumberAsc(Integer planId);

    List<PlanMilestoneEntity> findByPlanIdAndStatusNotOrderByWeekNumberAsc(Integer planId, String status);

    Optional<PlanMilestoneEntity> findByIdAndProjectId(Integer id, Integer projectId);

    List<PlanMilestoneEntity> findByProjectIdAndStatus(Integer projectId, String status);
}

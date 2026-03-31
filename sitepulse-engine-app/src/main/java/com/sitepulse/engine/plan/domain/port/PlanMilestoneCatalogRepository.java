package com.sitepulse.engine.plan.domain.port;

import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import java.util.List;
import java.util.Optional;

public interface PlanMilestoneCatalogRepository {

    List<PlanMilestone> findByPlanId(Integer planId);

    List<PlanMilestone> findByPlanIdAndStatusNot(Integer planId, MilestoneStatus status);

    Optional<PlanMilestone> findByIdAndProjectId(Integer milestoneId, Integer projectId);

    PlanMilestone save(PlanMilestone milestone);
}

package com.sitepulse.engine.plan.domain.port;

import com.sitepulse.engine.plan.domain.model.ConstructionPlan;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.model.PlanStatus;
import java.util.List;
import java.util.Optional;

public interface ConstructionPlanCatalogRepository {

    ConstructionPlan save(ConstructionPlan plan);

    Optional<ConstructionPlan> findLatestByProjectId(Integer projectId);

    Optional<ConstructionPlan> findLatestByProjectIdAndStatus(Integer projectId, PlanStatus status);

    ConstructionPlan saveUploadedPlan(ConstructionPlan plan, List<PlanMilestone> milestones);
}

package com.sitepulse.engine.plan.application;

import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.application.result.PlanSummaryResult;
import com.sitepulse.engine.plan.domain.model.ConstructionPlan;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlanResultMapper {

    public PlanMilestoneResult toMilestoneResult(PlanMilestone milestone) {
        return new PlanMilestoneResult(
                milestone.getId(),
                milestone.getWeekNumber(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getExpectedState(),
                milestone.getActualState(),
                milestone.getStatus(),
                milestone.getCheckedAt(),
                milestone.getCreatedAt()
        );
    }

    public PlanSummaryResult toSummaryResult(ConstructionPlan plan, List<PlanMilestone> milestones) {
        return new PlanSummaryResult(
                plan.getId(),
                plan.getFilename(),
                plan.getStatus(),
                plan.getCreatedAt(),
                milestones.stream().map(this::toMilestoneResult).toList()
        );
    }
}

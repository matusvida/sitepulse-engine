package com.sitepulse.engine.plan.infrastructure.persistence;

import com.sitepulse.engine.plan.domain.PlanMilestoneEntity;
import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;

final class PlanMilestoneEntityMapper {

    private PlanMilestoneEntityMapper() {
    }

    static PlanMilestoneEntity toEntity(PlanMilestone milestone, Integer planId) {
        PlanMilestoneEntity entity = new PlanMilestoneEntity();
        entity.setId(milestone.getId());
        entity.setPlanId(planId);
        entity.setProjectId(milestone.getProjectId());
        entity.setWeekNumber(milestone.getWeekNumber());
        entity.setTitle(milestone.getTitle());
        entity.setDescription(milestone.getDescription());
        entity.setExpectedState(milestone.getExpectedState());
        entity.setActualState(milestone.getActualState());
        entity.setStatus(milestone.getStatus().toPersistenceValue());
        entity.setCheckedAt(milestone.getCheckedAt());
        entity.setCreatedAt(milestone.getCreatedAt());
        return entity;
    }

    static PlanMilestone toDomain(PlanMilestoneEntity entity) {
        return PlanMilestone.restore(
                entity.getId(),
                entity.getPlanId(),
                entity.getProjectId(),
                entity.getWeekNumber(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getExpectedState(),
                entity.getActualState(),
                MilestoneStatus.fromValue(entity.getStatus()),
                entity.getCheckedAt(),
                entity.getCreatedAt()
        );
    }
}

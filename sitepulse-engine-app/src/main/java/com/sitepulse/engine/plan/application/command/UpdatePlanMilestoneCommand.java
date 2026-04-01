package com.sitepulse.engine.plan.application.command;

import com.sitepulse.engine.plan.domain.model.MilestoneStatus;

public record UpdatePlanMilestoneCommand(
        Integer projectId,
        Integer milestoneId,
        String title,
        String description,
        String expectedState,
        MilestoneStatus status
) {
}

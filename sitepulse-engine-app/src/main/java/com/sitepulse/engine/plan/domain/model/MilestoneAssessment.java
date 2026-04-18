package com.sitepulse.engine.plan.domain.model;

import com.sitepulse.engine.plan.domain.enums.MilestoneStatus;

public record MilestoneAssessment(
        MilestoneStatus status,
        String actualState
) {
}

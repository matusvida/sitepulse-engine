package com.sitepulse.engine.plan.domain.model;

public record MilestoneAssessment(
        MilestoneStatus status,
        String actualState
) {
}

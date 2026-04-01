package com.sitepulse.engine.plan.domain.model;

public record ParsedMilestone(
        int weekNumber,
        String title,
        String description,
        String expectedState
) {
}

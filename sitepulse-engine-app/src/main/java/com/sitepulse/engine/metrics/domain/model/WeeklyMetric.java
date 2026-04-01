package com.sitepulse.engine.metrics.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class WeeklyMetric {

    private final Integer id;
    private final Integer projectId;

    @EqualsAndHashCode.Include
    @ToString.Include
    private final LocalDate weekStart;

    private Double progressDelta;
    private Double activityIndex;
    private Double activeHours;
    private RiskLevel riskLevel;
    private final OffsetDateTime createdAt;

    public static WeeklyMetric create(Integer projectId, LocalDate weekStart, OffsetDateTime createdAt) {
        return new WeeklyMetric(null, projectId, weekStart, 0.0, 0.0, 0.0, RiskLevel.LOW, createdAt);
    }

    public static WeeklyMetric restore(
            Integer id,
            Integer projectId,
            LocalDate weekStart,
            Double progressDelta,
            Double activityIndex,
            Double activeHours,
            RiskLevel riskLevel,
            OffsetDateTime createdAt
    ) {
        return new WeeklyMetric(id, projectId, weekStart, progressDelta, activityIndex, activeHours, riskLevel, createdAt);
    }

    public void updateSummary(double progressDelta, double activityIndex, double activeHours, RiskLevel riskLevel) {
        this.progressDelta = progressDelta;
        this.activityIndex = activityIndex;
        this.activeHours = activeHours;
        this.riskLevel = riskLevel;
    }
}

package com.sitepulse.engine.metrics.domain.model;

import com.sitepulse.engine.metrics.domain.enums.RiskLevel;
import java.math.BigDecimal;
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

    private BigDecimal progressDelta;
    private BigDecimal activityIndex;
    private Double activeHours;
    private RiskLevel riskLevel;
    private final OffsetDateTime createdAt;

    public static WeeklyMetric create(Integer projectId, LocalDate weekStart, OffsetDateTime createdAt) {
        return new WeeklyMetric(null, projectId, weekStart, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, RiskLevel.LOW, createdAt);
    }

    public static WeeklyMetric restore(
            Integer id,
            Integer projectId,
            LocalDate weekStart,
            BigDecimal progressDelta,
            BigDecimal activityIndex,
            Double activeHours,
            RiskLevel riskLevel,
            OffsetDateTime createdAt
    ) {
        return new WeeklyMetric(id, projectId, weekStart, progressDelta, activityIndex, activeHours, riskLevel, createdAt);
    }

    public void updateSummary(BigDecimal progressDelta, BigDecimal activityIndex, double activeHours, RiskLevel riskLevel) {
        this.progressDelta = progressDelta;
        this.activityIndex = activityIndex;
        this.activeHours = activeHours;
        this.riskLevel = riskLevel;
    }
}

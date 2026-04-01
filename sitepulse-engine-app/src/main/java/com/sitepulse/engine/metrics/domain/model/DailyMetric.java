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
public class DailyMetric {

    private final Integer id;
    private final Integer projectId;

    @EqualsAndHashCode.Include
    @ToString.Include
    private final LocalDate date;

    private Integer peopleCount;
    private Integer vehicleCount;
    private Double activeHours;
    private final OffsetDateTime createdAt;

    public static DailyMetric create(Integer projectId, LocalDate date, OffsetDateTime createdAt) {
        return new DailyMetric(null, projectId, date, 0, 0, 0.0, createdAt);
    }

    public static DailyMetric restore(
            Integer id,
            Integer projectId,
            LocalDate date,
            Integer peopleCount,
            Integer vehicleCount,
            Double activeHours,
            OffsetDateTime createdAt
    ) {
        return new DailyMetric(id, projectId, date, peopleCount, vehicleCount, activeHours, createdAt);
    }

    public void updateCounts(int peopleCount, int vehicleCount, double activeHours) {
        this.peopleCount = peopleCount;
        this.vehicleCount = vehicleCount;
        this.activeHours = activeHours;
    }
}

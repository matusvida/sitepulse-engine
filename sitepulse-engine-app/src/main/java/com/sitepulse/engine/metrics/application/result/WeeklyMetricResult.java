package com.sitepulse.engine.metrics.application.result;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class WeeklyMetricResult {

    private LocalDate weekStart;
    private double progressDelta;
    private double activityIndex;
    private double activeHours;
    private String riskLevel;
}

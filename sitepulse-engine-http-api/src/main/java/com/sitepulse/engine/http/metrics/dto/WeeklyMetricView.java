package com.sitepulse.engine.http.metrics.dto;

import java.math.BigDecimal;
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
public class WeeklyMetricView {

    private String weekStart;
    private BigDecimal progressDelta;
    private BigDecimal activityIndex;
    private double activeHours;
    private String riskLevel;
}

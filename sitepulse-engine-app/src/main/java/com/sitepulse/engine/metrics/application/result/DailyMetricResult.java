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
public class DailyMetricResult {

    private LocalDate date;
    private int peopleCount;
    private int vehicleCount;
    private double activeHours;
}

package com.sitepulse.engine.metrics.application.result;

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
public class ActivitySummaryResult {

    private int totalDays;
    private int activeDays;
    private int inactiveDays;
    private int unknownDays;
    private int weatherImpactedDays;
    private int rainDays;
    private int snowDays;
}

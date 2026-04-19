package com.sitepulse.engine.http.metrics.dto;

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
public class ActivitySummaryView {

    private int totalDays;
    private int activeDays;
    private int inactiveDays;
    private int unknownDays;
    private int weatherImpactedDays;
    private int rainDays;
    private int snowDays;
}

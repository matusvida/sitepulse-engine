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
public class DailyMetricView {

    private String date;
    private int peopleCount;
    private int vehicleCount;
    private double activeHours;
    private String activityStatus;
    private String activityConfidence;
    private String weatherStatus;
    private boolean weatherImpacted;
    private java.util.List<String> reasonCodes;
    private String summaryNote;
}

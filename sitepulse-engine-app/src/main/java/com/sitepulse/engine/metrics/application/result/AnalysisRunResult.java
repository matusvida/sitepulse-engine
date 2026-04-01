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
public class AnalysisRunResult {

    private Integer projectId;
    private int daysProcessed;
    private int weeksProcessed;
    private int lookbackDays;
}

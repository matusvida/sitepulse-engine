package com.sitepulse.engine.report.domain.event;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ProgressReportGeneratedEvent {

    private final Integer reportId;
    private final Integer projectId;
    private final String reportType;
    private final LocalDate dateRangeStart;
    private final LocalDate dateRangeEnd;
}

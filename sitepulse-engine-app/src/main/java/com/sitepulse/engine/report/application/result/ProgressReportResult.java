package com.sitepulse.engine.report.application.result;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
public class ProgressReportResult {

    private Integer id;
    private Integer projectId;
    private String reportType;
    private String summary;
    private LocalDate dateRangeStart;
    private LocalDate dateRangeEnd;
    private Integer imageCount;
    private String modelUsed;
    private OffsetDateTime createdAt;
    private String contentMd;
}

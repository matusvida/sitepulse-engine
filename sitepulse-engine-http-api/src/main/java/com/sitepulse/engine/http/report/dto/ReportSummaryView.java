package com.sitepulse.engine.http.report.dto;

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
public class ReportSummaryView {

    private Integer id;
    private String reportType;
    private String summary;
    private String dateRangeStart;
    private String dateRangeEnd;
    private Integer imageCount;
    private String modelUsed;
    private String createdAt;
}

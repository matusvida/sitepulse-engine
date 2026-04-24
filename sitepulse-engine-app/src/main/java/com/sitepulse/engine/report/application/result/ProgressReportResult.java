package com.sitepulse.engine.report.application.result;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
    private String generationOrigin;
    private String confidenceLevel;
    private String language;
    private String periodLabel;
    private String headline;
    private String summary;
    private LocalDate dateRangeStart;
    private LocalDate dateRangeEnd;
    private Integer imageCount;
    private Integer evidenceImageCount;
    private String modelUsed;
    private OffsetDateTime createdAt;
    private String contentMd;
    private List<ReportEvidenceImageResult> evidenceImages;
}

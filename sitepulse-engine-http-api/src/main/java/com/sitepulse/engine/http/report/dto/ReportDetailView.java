package com.sitepulse.engine.http.report.dto;

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
public class ReportDetailView {

    private Integer id;
    private String reportType;
    private String generationOrigin;
    private String confidenceLevel;
    private String language;
    private String periodLabel;
    private String headline;
    private String summary;
    private String dateRangeStart;
    private String dateRangeEnd;
    private Integer imageCount;
    private Integer evidenceImageCount;
    private String modelUsed;
    private String createdAt;
    private String projectId;
    private String contentMd;
    private List<ReportEvidenceImageView> evidenceImages;
}

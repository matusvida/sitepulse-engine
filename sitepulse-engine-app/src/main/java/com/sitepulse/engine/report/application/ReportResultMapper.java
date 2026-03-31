package com.sitepulse.engine.report.application;

import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import org.springframework.stereotype.Component;

@Component
public class ReportResultMapper {

    public ProgressReportResult toResult(ProgressReport report) {
        return new ProgressReportResult(
                report.getId(),
                report.getProjectId(),
                report.getReportType(),
                report.getSummary(),
                report.getDateRangeStart(),
                report.getDateRangeEnd(),
                report.getImageCount(),
                report.getModelUsed(),
                report.getCreatedAt(),
                report.getContentMd()
        );
    }
}

package com.sitepulse.engine.report.application;

import com.sitepulse.engine.report.application.result.ProgressReportResult;
import java.util.List;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ReportResultMapper {

    public ProgressReportResult toResult(ProgressReport report) {
        return new ProgressReportResult(
                report.getId(),
                report.getProjectId(),
                report.getReportType(),
                report.getGenerationOrigin(),
                report.getConfidenceLevel(),
                periodLabel(report),
                report.getHeadline(),
                report.getSummary(),
                report.getDateRangeStart(),
                report.getDateRangeEnd(),
                report.getImageCount(),
                report.getEvidenceImageCount(),
                report.getModelUsed(),
                report.getCreatedAt(),
                report.getContentMd(),
                List.of()
        );
    }

    private String periodLabel(ProgressReport report) {
        LocalDate from = report.getDateRangeStart();
        LocalDate to = report.getDateRangeEnd();
        if (from == null && to == null) {
            return null;
        }
        if (from != null && to != null) {
            if (from.equals(to)) {
                return from.toString();
            }
            if ("weekly".equalsIgnoreCase(report.getReportType())) {
                return "Week of " + from;
            }
            return from + " - " + to;
        }
        return from != null ? from.toString() : to.toString();
    }
}

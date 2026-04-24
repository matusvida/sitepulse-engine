package com.sitepulse.engine.report.application;

import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
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
                report.getLanguage() == null ? "SK" : report.getLanguage().toUpperCase(),
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
                if (!from.plusDays(6).equals(to) || from.getDayOfWeek() != DayOfWeek.MONDAY) {
                    return from + " - " + to;
                }
                return "Week of " + from;
            }
            return from + " - " + to;
        }
        return from != null ? from.toString() : to.toString();
    }
}

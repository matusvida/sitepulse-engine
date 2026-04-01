package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.metrics.application.result.DailyMetricResult;
import com.sitepulse.engine.metrics.application.result.WeeklyMetricResult;
import com.sitepulse.engine.metrics.application.usecase.ListDailyMetricsQuery;
import com.sitepulse.engine.metrics.application.usecase.ListWeeklyMetricsQuery;
import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.application.usecase.GetLatestPlanQuery;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.event.ProgressReportGeneratedEvent;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import com.sitepulse.engine.report.domain.port.ReportGenerator;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerateProgressReportUseCase {

    private final ProjectLookupService projectLookupService;
    private final ReportEvidenceImageProvider reportEvidenceImageProvider;
    private final ListDailyMetricsQuery listDailyMetricsQuery;
    private final ListWeeklyMetricsQuery listWeeklyMetricsQuery;
    private final GetLatestPlanQuery getLatestPlanQuery;
    private final ReportGenerator reportGenerator;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final ReportResultMapper reportResultMapper;
    private final DomainEventPublisher domainEventPublisher;

    public ProgressReportResult generate(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        projectLookupService.requireProject(projectId);
        if (dateFrom.isAfter(dateTo)) {
            throw new ValidationException("dateFrom must be <= dateTo");
        }
        List<ReportImageEvidence> imageData = reportEvidenceImageProvider.gather(projectId, dateFrom, dateTo, 8);
        if (imageData.isEmpty()) {
            throw new ProcessingException("No images found in the given date range. Run a sync first.");
        }
        String content = reportGenerator.generate(
                imageData,
                buildMetricsContext(projectId, dateFrom, dateTo),
                buildMilestonesContext(projectId)
        );
        String summary = content.isBlank() ? "" : content.substring(0, Math.min(300, content.length())).split("\n")[0];
        ProgressReport report = ProgressReport.create(
                projectId,
                "custom",
                content,
                summary,
                dateFrom,
                dateTo,
                imageData.size(),
                "gpt-4o",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        ProgressReport savedReport = progressReportCatalogRepository.save(report);
        domainEventPublisher.publish(new ProgressReportGeneratedEvent(
                savedReport.getId(),
                savedReport.getProjectId(),
                savedReport.getReportType(),
                savedReport.getDateRangeStart(),
                savedReport.getDateRangeEnd()
        ));
        return reportResultMapper.toResult(savedReport);
    }

    private String buildMetricsContext(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        StringBuilder builder = new StringBuilder();
        builder.append("### Daily Metrics\n");
        List<DailyMetricResult> dailyMetrics = listDailyMetricsQuery.list(
                projectId,
                (int) ChronoUnit.DAYS.between(dateFrom, dateTo) + 1
        );
        dailyMetrics.forEach(row -> builder.append("- ").append(row.getDate())
                .append(": people=").append(row.getPeopleCount())
                .append(", vehicles=").append(row.getVehicleCount())
                .append(", active_hours=").append(row.getActiveHours())
                .append('\n'));

        builder.append("\n### Weekly Metrics\n");
        List<WeeklyMetricResult> weeklyMetrics = listWeeklyMetricsQuery.list(projectId, 12);
        weeklyMetrics.forEach(row -> builder.append("- Week of ").append(row.getWeekStart())
                .append(": progress_delta=").append(row.getProgressDelta())
                .append(", activity_index=").append(row.getActivityIndex())
                .append(", active_hours=").append(row.getActiveHours())
                .append(", risk=").append(row.getRiskLevel())
                .append('\n'));
        return builder.toString();
    }

    private String buildMilestonesContext(Integer projectId) {
        return getLatestPlanQuery.get(projectId)
                .map(result -> {
                    if (result.getMilestones().isEmpty()) {
                        return "No construction plan uploaded.";
                    }
                    StringBuilder builder = new StringBuilder("### Construction Plan Milestones\n");
                    for (PlanMilestoneResult milestone : result.getMilestones()) {
                        builder.append("- Week ").append(milestone.getWeekNumber())
                                .append(": ").append(milestone.getTitle())
                                .append(" (status: ").append(milestone.getStatus().toPersistenceValue()).append(')');
                        if (milestone.getExpectedState() != null) {
                            builder.append("\n  Expected: ").append(milestone.getExpectedState());
                        }
                        if (milestone.getActualState() != null) {
                            builder.append("\n  Actual: ").append(milestone.getActualState());
                        }
                        builder.append('\n');
                    }
                    return builder.toString();
                })
                .orElse("No construction plan uploaded.");
    }
}

package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.service.DailyReportSummary;
import com.sitepulse.engine.report.application.service.DailyReportSummaryBuilder;
import com.sitepulse.engine.report.application.service.ReportEvidenceQueryService;
import com.sitepulse.engine.report.application.service.ReportCompositionService;
import com.sitepulse.engine.report.application.service.WeeklyReportSummary;
import com.sitepulse.engine.report.application.service.WeeklyReportSummaryBuilder;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.GenerationOrigin;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.enums.ReportType;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.domain.port.ReportContextProvider;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateProgressReportUseCase {

    private final ProjectLookupService projectLookupService;
    private final ReportContextProvider reportContextProvider;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final DailyReportSummaryBuilder dailyReportSummaryBuilder;
    private final WeeklyReportSummaryBuilder weeklyReportSummaryBuilder;
    private final ReportEvidenceQueryService reportEvidenceQueryService;
    private final ReportCompositionService reportCompositionService;
    private final ReportResultMapper reportResultMapper;

    public ProgressReportResult generate(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        projectLookupService.requireProject(projectId);
        if (dateFrom.isAfter(dateTo)) {
            throw new ValidationException("dateFrom must be <= dateTo");
        }
        return generateCustom(projectId, dateFrom, dateTo);
    }

    public Optional<ProgressReportResult> generateAutomaticDaily(Integer projectId, LocalDate date) {
        Project project = projectLookupService.requireProject(projectId);
        String periodKey = "daily:" + date;
        Optional<ProgressReport> existing = progressReportCatalogRepository.findByProjectAndPeriodKey(projectId, periodKey);
        if (existing.isPresent()) {
            return existing.map(report -> attachEvidence(reportResultMapper.toResult(report), projectId, date, date, 4));
        }
        OffsetDateTime fromUtc = startOfDayUtc(date, zone(project));
        OffsetDateTime toUtc = startOfDayUtc(date.plusDays(1), zone(project));
        DailyReportSummary summary = dailyReportSummaryBuilder.build(projectId, date, fromUtc, toUtc);
        if (summary.imageCount() == 0) {
            return Optional.empty();
        }
        ProgressReportResult result = generateStructured(
                projectId,
                ReportType.DAILY,
                GenerationOrigin.AUTOMATIC,
                date,
                date,
                periodKey,
                summary.confidenceLevel(),
                summary.contextText(),
                4
        );
        return Optional.of(attachEvidence(result, projectId, date, date, 4));
    }

    public Optional<ProgressReportResult> generateAutomaticWeekly(Integer projectId, LocalDate weekStart) {
        Project project = projectLookupService.requireProject(projectId);
        String periodKey = "weekly:" + weekStart;
        Optional<ProgressReport> existing = progressReportCatalogRepository.findByProjectAndPeriodKey(projectId, periodKey);
        if (existing.isPresent()) {
            return existing.map(report -> attachEvidence(reportResultMapper.toResult(report), projectId, weekStart, weekStart.plusDays(6), 6));
        }
        Map<LocalDate, OffsetDateTime> fromByDay = new LinkedHashMap<>();
        Map<LocalDate, OffsetDateTime> toByDay = new LinkedHashMap<>();
        for (int index = 0; index < 7; index++) {
            LocalDate day = weekStart.plusDays(index);
            fromByDay.put(day, startOfDayUtc(day, zone(project)));
            toByDay.put(day, startOfDayUtc(day.plusDays(1), zone(project)));
        }
        WeeklyReportSummary summary = weeklyReportSummaryBuilder.build(projectId, weekStart, fromByDay, toByDay);
        if (summary.imageCount() == 0 || summary.activeDays() == 0) {
            return Optional.empty();
        }
        ProgressReportResult result = generateStructured(
                projectId,
                ReportType.WEEKLY,
                GenerationOrigin.AUTOMATIC,
                weekStart,
                weekStart.plusDays(6),
                periodKey,
                summary.confidenceLevel(),
                summary.contextText(),
                6
        );
        return Optional.of(attachEvidence(result, projectId, weekStart, weekStart.plusDays(6), 6));
    }

    private ProgressReportResult generateCustom(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        int days = (int) ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;
        String metricsContext = reportContextProvider.getMetricsSummary(projectId, days);
        String milestonesContext = reportContextProvider.getMilestoneSummary(projectId);
        ProgressReport report = reportCompositionService.composeAndSave(
                projectId,
                ReportType.CUSTOM,
                GenerationOrigin.MANUAL,
                dateFrom,
                dateTo,
                periodKey(dateFrom, dateTo),
                null,
                metricsContext,
                milestonesContext,
                8
        );
        return attachEvidence(reportResultMapper.toResult(report), projectId, dateFrom, dateTo, 8);
    }

    private ProgressReportResult generateStructured(
            Integer projectId,
            ReportType reportType,
            GenerationOrigin generationOrigin,
            LocalDate dateFrom,
            LocalDate dateTo,
            String periodKey,
            ConfidenceLevel confidenceLevel,
            String structuredContext,
            int maxImages
    ) {
        String milestonesContext = reportContextProvider.getMilestoneSummary(projectId);
        ProgressReport report = reportCompositionService.composeAndSave(
                projectId,
                reportType,
                generationOrigin,
                dateFrom,
                dateTo,
                periodKey,
                confidenceLevel,
                structuredContext,
                milestonesContext,
                maxImages
        );
        return attachEvidence(reportResultMapper.toResult(report), projectId, dateFrom, dateTo, maxImages);
    }

    private String periodKey(LocalDate dateFrom, LocalDate dateTo) {
        return "custom:" + dateFrom + ":" + dateTo;
    }

    private ProgressReportResult attachEvidence(
            ProgressReportResult result,
            Integer projectId,
            LocalDate dateFrom,
            LocalDate dateTo,
            int maxImages
    ) {
        result.setEvidenceImages(reportEvidenceQueryService.list(projectId, dateFrom, dateTo, maxImages));
        return result;
    }

    public LocalDate previousCompletedWeekStart(Project project, ZonedDateTime now) {
        LocalDate localDate = now.withZoneSameInstant(zone(project)).toLocalDate();
        LocalDate currentWeekStart = localDate.minusDays((long) localDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        return currentWeekStart.minusWeeks(1);
    }

    public LocalDate previousCompletedDay(Project project, ZonedDateTime now) {
        return now.withZoneSameInstant(zone(project)).toLocalDate().minusDays(1);
    }

    private ZoneId zone(Project project) {
        return ZoneId.of(project.getTimezone() == null || project.getTimezone().isBlank() ? "UTC" : project.getTimezone());
    }

    private OffsetDateTime startOfDayUtc(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }
}

package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
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
import com.sitepulse.engine.report.domain.enums.ReportLanguage;
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
    private final ProcessedImageReadModel processedImageReadModel;
    private final ReportContextProvider reportContextProvider;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final DailyReportSummaryBuilder dailyReportSummaryBuilder;
    private final WeeklyReportSummaryBuilder weeklyReportSummaryBuilder;
    private final ReportEvidenceQueryService reportEvidenceQueryService;
    private final ReportCompositionService reportCompositionService;
    private final ReportResultMapper reportResultMapper;

    public ProgressReportResult generate(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        return generate(projectId, dateFrom, dateTo, ReportLanguage.SK);
    }

    public ProgressReportResult generate(Integer projectId, LocalDate dateFrom, LocalDate dateTo, ReportLanguage language) {
        projectLookupService.requireProject(projectId);
        if (dateFrom.isAfter(dateTo)) {
            throw new ValidationException("dateFrom must be <= dateTo");
        }
        return generateCustom(projectId, dateFrom, dateTo, language);
    }

    public Optional<ProgressReportResult> generateAutomaticDaily(Integer projectId, LocalDate date) {
        return generateAutomaticDaily(projectId, date, ReportLanguage.SK);
    }

    public Optional<ProgressReportResult> generateAutomaticDaily(Integer projectId, LocalDate date, ReportLanguage language) {
        Project project = projectLookupService.requireProject(projectId);
        String periodKey = "daily:" + date;
        Optional<ProgressReport> existing = progressReportCatalogRepository.findByProjectAndPeriodKeyAndLanguage(
                projectId,
                periodKey,
                language.toPersistenceValue()
        );
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
                language,
                summary.contextText(),
                4
        );
        return Optional.of(attachEvidence(result, projectId, date, date, 4));
    }

    public ProgressReportResult generateAutomaticDailyOnDemand(Integer projectId, LocalDate date) {
        return generateAutomaticDailyOnDemand(projectId, date, ReportLanguage.SK);
    }

    public ProgressReportResult generateAutomaticDailyOnDemand(Integer projectId, LocalDate date, ReportLanguage language) {
        return generateAutomaticDaily(projectId, date, language)
                .orElseThrow(() -> new ValidationException("No processed images found for daily report on " + date));
    }

    public Optional<ProgressReportResult> generateAutomaticWeekly(Integer projectId, LocalDate weekStart) {
        return generateAutomaticWeekly(projectId, weekStart, ReportLanguage.SK);
    }

    public Optional<ProgressReportResult> generateAutomaticWeekly(Integer projectId, LocalDate weekStart, ReportLanguage language) {
        Project project = projectLookupService.requireProject(projectId);
        WeeklyPeriod period = resolveWeeklyPeriod(projectId, project, weekStart);
        String periodKey = weeklyPeriodKey(period.dateFrom(), period.dateTo());
        Optional<ProgressReport> existing = progressReportCatalogRepository.findByProjectAndPeriodKeyAndLanguage(
                projectId,
                periodKey,
                language.toPersistenceValue()
        );
        if (existing.isPresent()) {
            return existing.map(report -> attachEvidence(reportResultMapper.toResult(report), projectId, period.dateFrom(), period.dateTo(), 6));
        }
        Map<LocalDate, OffsetDateTime> fromByDay = new LinkedHashMap<>();
        Map<LocalDate, OffsetDateTime> toByDay = new LinkedHashMap<>();
        for (LocalDate day = period.dateFrom(); !day.isAfter(period.dateTo()); day = day.plusDays(1)) {
            fromByDay.put(day, startOfDayUtc(day, zone(project)));
            toByDay.put(day, startOfDayUtc(day.plusDays(1), zone(project)));
        }
        WeeklyReportSummary summary = weeklyReportSummaryBuilder.build(projectId, period.dateFrom(), period.dateTo(), fromByDay, toByDay);
        if (summary.imageCount() == 0 || summary.activeDays() == 0) {
            return Optional.empty();
        }
        ProgressReportResult result = generateStructured(
                projectId,
                ReportType.WEEKLY,
                GenerationOrigin.AUTOMATIC,
                period.dateFrom(),
                period.dateTo(),
                periodKey,
                summary.confidenceLevel(),
                language,
                summary.contextText(),
                6
        );
        return Optional.of(attachEvidence(result, projectId, period.dateFrom(), period.dateTo(), 6));
    }

    public ProgressReportResult generateAutomaticWeeklyOnDemand(Integer projectId, LocalDate date) {
        return generateAutomaticWeeklyOnDemand(projectId, date, ReportLanguage.SK);
    }

    public ProgressReportResult generateAutomaticWeeklyOnDemand(Integer projectId, LocalDate date, ReportLanguage language) {
        Project project = projectLookupService.requireProject(projectId);
        WeeklyPeriod period = resolveWeeklyPeriod(projectId, project, date);
        return generateAutomaticWeekly(projectId, date, language)
                .orElseThrow(() -> new ValidationException(
                        "No processed images found for weekly report in range " + period.dateFrom() + " to " + period.dateTo()
                ));
    }

    private ProgressReportResult generateCustom(Integer projectId, LocalDate dateFrom, LocalDate dateTo, ReportLanguage language) {
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
                language,
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
            ReportLanguage language,
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
                language,
                structuredContext,
                milestonesContext,
                maxImages
        );
        return attachEvidence(reportResultMapper.toResult(report), projectId, dateFrom, dateTo, maxImages);
    }

    private String periodKey(LocalDate dateFrom, LocalDate dateTo) {
        return "custom:" + dateFrom + ":" + dateTo;
    }

    private String weeklyPeriodKey(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom.getDayOfWeek() == DayOfWeek.MONDAY && dateTo.equals(dateFrom.plusDays(6))) {
            return "weekly:" + dateFrom;
        }
        return "weekly:" + dateFrom + ":" + dateTo;
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

    private WeeklyPeriod resolveWeeklyPeriod(Integer projectId, Project project, LocalDate date) {
        LocalDate weekStart = date.minusDays((long) date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate weekEnd = weekStart.plusDays(6);
        ZoneId zone = zone(project);
        LocalDate effectiveStart = processedImageReadModel.findSnapshotCapturedAtValues(projectId).stream()
                .min(OffsetDateTime::compareTo)
                .map(capturedAt -> capturedAt.atZoneSameInstant(zone).toLocalDate())
                .filter(firstCapturedDate -> firstCapturedDate.isAfter(weekStart) && !firstCapturedDate.isAfter(weekEnd))
                .orElse(weekStart);
        return new WeeklyPeriod(effectiveStart, weekEnd);
    }

    private OffsetDateTime startOfDayUtc(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private record WeeklyPeriod(LocalDate dateFrom, LocalDate dateTo) {
    }
}

package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.alert.application.command.CreateAlertCommand;
import com.sitepulse.engine.alert.application.usecase.AutoResolveAlertsUseCase;
import com.sitepulse.engine.alert.application.usecase.CreateAlertUseCase;
import com.sitepulse.engine.alert.domain.model.AlertSeverity;
import com.sitepulse.engine.metrics.application.result.AnalysisRunResult;
import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.model.DetectionActivitySample;
import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import com.sitepulse.engine.metrics.domain.port.DetectionMetricsReadModel;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import com.sitepulse.engine.metrics.domain.policy.DeclineDetectionPolicy;
import com.sitepulse.engine.metrics.domain.policy.StallDetectionPolicy;
import com.sitepulse.engine.metrics.domain.service.DailyActivityAggregator;
import com.sitepulse.engine.metrics.domain.service.WeeklyRollupCalculator;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunProjectAnalysisUseCase {

    private final ProjectLookupService projectLookupService;
    private final DailyMetricCatalogRepository dailyMetricCatalogRepository;
    private final WeeklyMetricCatalogRepository weeklyMetricCatalogRepository;
    private final DetectionMetricsReadModel detectionMetricsReadModel;
    private final CreateAlertUseCase createAlertUseCase;
    private final AutoResolveAlertsUseCase autoResolveAlertsUseCase;

    private final DailyActivityAggregator dailyAggregator = new DailyActivityAggregator();
    private final WeeklyRollupCalculator weeklyCalculator = new WeeklyRollupCalculator();
    private final StallDetectionPolicy stallPolicy = new StallDetectionPolicy();
    private final DeclineDetectionPolicy declinePolicy = new DeclineDetectionPolicy();

    @Transactional
    public AnalysisRunResult run(Integer projectId, int lookbackDays) {
        projectLookupService.requireProject(projectId);
        log.info("Running analysis for projectId={} lookbackDays={}", projectId, lookbackDays);

        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(lookbackDays);
        int daysProcessed = 0;
        for (LocalDate date : detectionMetricsReadModel.findProcessedDates(projectId, cutoff)) {
            if (aggregateDay(projectId, date)) {
                daysProcessed++;
            }
        }

        int weeksProcessed = 0;
        for (LocalDate weekStart : detectionMetricsReadModel.findCompletedWeeks(projectId, cutoff.minusDays(7))) {
            if (rollupWeek(projectId, weekStart)) {
                weeksProcessed++;
            }
        }

        generateAlerts(projectId);
        return new AnalysisRunResult(projectId, daysProcessed, weeksProcessed, lookbackDays);
    }

    private boolean aggregateDay(Integer projectId, LocalDate targetDate) {
        List<DetectionActivitySample> rows = detectionMetricsReadModel.findDetectionActivityForDay(projectId, targetDate);
        if (rows.isEmpty()) {
            return false;
        }

        DailyActivityAggregator.DailyAggregation aggregation = dailyAggregator.aggregate(rows);
        DailyMetric metric = dailyMetricCatalogRepository.findByProjectAndDate(projectId, targetDate)
                .orElse(DailyMetric.create(projectId, targetDate, OffsetDateTime.now(ZoneOffset.UTC)));
        metric.updateCounts(aggregation.peopleCount(), aggregation.vehicleCount(), aggregation.activeHours());
        dailyMetricCatalogRepository.save(metric);
        return true;
    }

    private boolean rollupWeek(Integer projectId, LocalDate weekStart) {
        List<DailyMetric> currentWeek = dailyMetricCatalogRepository.findBetween(projectId, weekStart, weekStart.plusDays(6));
        if (currentWeek.isEmpty()) {
            return false;
        }

        List<DailyMetric> previousWeek = dailyMetricCatalogRepository.findBetween(projectId, weekStart.minusDays(7), weekStart.minusDays(1));
        List<DailyMetric> allHistoric = dailyMetricCatalogRepository.findAllSince(projectId, LocalDate.of(2000, 1, 1));
        Double rollingAverage = weeklyMetricCatalogRepository.findAverageActivityBefore(projectId, weekStart);

        WeeklyRollupCalculator.WeeklyRollup rollup = weeklyCalculator.calculate(currentWeek, previousWeek, allHistoric, rollingAverage);

        WeeklyMetric metric = weeklyMetricCatalogRepository.findByProjectAndWeekStart(projectId, weekStart)
                .orElse(WeeklyMetric.create(projectId, weekStart, OffsetDateTime.now(ZoneOffset.UTC)));
        metric.updateSummary(rollup.progressDelta(), rollup.activityIndex(), rollup.activeHours(), rollup.riskLevel());
        weeklyMetricCatalogRepository.save(metric);
        return true;
    }

    private void generateAlerts(Integer projectId) {
        List<DailyMetric> daily = dailyMetricCatalogRepository.findSince(projectId, LocalDate.now(ZoneOffset.UTC).minusDays(14));
        List<WeeklyMetric> weekly = weeklyMetricCatalogRepository.findLatest(projectId, 4);

        long consecutiveLow = stallPolicy.consecutiveLowActivityDays(daily);
        if (stallPolicy.isStalled(daily)) {
            createAlertUseCase.create(new CreateAlertCommand(
                    projectId,
                    "stall",
                    AlertSeverity.HIGH,
                    "No significant activity detected for " + consecutiveLow + " consecutive days",
                    "Total detections have been at or below 2 for the last " + consecutiveLow + " days.",
                    List.of("Verify with site manager if work has been paused", "Check material delivery schedule", "Review weather logs")
            ));
        } else {
            autoResolveAlertsUseCase.resolve(projectId, "stall");
        }

        long consecutiveNegativeWeeks = declinePolicy.consecutiveNegativeWeeks(weekly);
        if (declinePolicy.isDeclining(weekly)) {
            createAlertUseCase.create(new CreateAlertCommand(
                    projectId,
                    "schedule",
                    consecutiveNegativeWeeks >= 3 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM,
                    "Activity declining for " + consecutiveNegativeWeeks + " consecutive weeks",
                    "Progress delta has been negative for " + consecutiveNegativeWeeks + " consecutive weeks.",
                    List.of("Review resource allocation", "Check for blocking issues", "Consider schedule adjustments")
            ));
        } else {
            autoResolveAlertsUseCase.resolve(projectId, "schedule");
        }
    }
}

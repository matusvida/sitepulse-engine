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
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunProjectAnalysisUseCase {

    private static final List<String> VEHICLE_CLASSES = List.of("car", "truck", "bus");
    private static final List<String> PERSON_CLASSES = List.of("person");

    private final ProjectLookupService projectLookupService;
    private final DailyMetricCatalogRepository dailyMetricCatalogRepository;
    private final WeeklyMetricCatalogRepository weeklyMetricCatalogRepository;
    private final DetectionMetricsReadModel detectionMetricsReadModel;
    private final CreateAlertUseCase createAlertUseCase;
    private final AutoResolveAlertsUseCase autoResolveAlertsUseCase;

    @Transactional
    public AnalysisRunResult run(Integer projectId, int lookbackDays) {
        projectLookupService.requireProject(projectId);
        log.info("Running analysis for projectId={} lookbackDays={}", projectId, lookbackDays);
        int daysProcessed = 0;
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(lookbackDays);
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

        Map<Integer, Integer> peoplePerImage = new HashMap<>();
        Map<Integer, Integer> vehiclePerImage = new HashMap<>();
        Map<Integer, Integer> hoursWithDetections = new HashMap<>();
        for (DetectionActivitySample row : rows) {
            if (PERSON_CLASSES.contains(row.getClassName())) {
                peoplePerImage.merge(row.getImageId(), 1, Integer::sum);
            } else if (VEHICLE_CLASSES.contains(row.getClassName())) {
                vehiclePerImage.merge(row.getImageId(), 1, Integer::sum);
            }
            hoursWithDetections.merge(row.getCapturedAt().getHour(), 1, Integer::sum);
        }

        int peopleCount = peoplePerImage.values().stream().max(Integer::compareTo).orElse(0);
        int vehicleCount = vehiclePerImage.values().stream().max(Integer::compareTo).orElse(0);
        double activeHours = hoursWithDetections.values().stream().filter(count -> count >= 3).count();
        DailyMetric metric = dailyMetricCatalogRepository.findByProjectAndDate(projectId, targetDate)
                .orElse(DailyMetric.create(projectId, targetDate, OffsetDateTime.now(ZoneOffset.UTC)));
        metric.updateCounts(peopleCount, vehicleCount, activeHours);
        dailyMetricCatalogRepository.save(metric);
        return true;
    }

    private boolean rollupWeek(Integer projectId, LocalDate weekStart) {
        List<DailyMetric> daily = dailyMetricCatalogRepository.findBetween(projectId, weekStart, weekStart.plusDays(6));
        if (daily.isEmpty()) {
            return false;
        }

        double totalActivity = daily.stream()
                .mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount())
                        + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()))
                .sum();
        double totalHours = daily.stream().mapToDouble(row -> row.getActiveHours() == null ? 0 : row.getActiveHours()).sum();
        double previousActivity = dailyMetricCatalogRepository.findBetween(projectId, weekStart.minusDays(7), weekStart.minusDays(1)).stream()
                .mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount())
                        + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()))
                .sum();
        double progressDelta = previousActivity > 0 ? ((totalActivity - previousActivity) / previousActivity) * 100.0 : (totalActivity > 0 ? 100.0 : 0.0);
        double maxActivity = dailyMetricCatalogRepository.findAllSince(projectId, LocalDate.of(2000, 1, 1)).stream()
                .collect(java.util.stream.Collectors.groupingBy(row -> row.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
                .values().stream()
                .mapToDouble(rows -> rows.stream()
                        .mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount())
                                + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()))
                        .sum())
                .max()
                .orElse(1.0);
        double activityIndex = Math.min(100.0, maxActivity == 0 ? 0 : (totalActivity / maxActivity) * 100.0);

        Double rollingAverage = weeklyMetricCatalogRepository.findAverageActivityBefore(projectId, weekStart);
        String riskLevel = "Low";
        if (rollingAverage != null && rollingAverage > 0) {
            double dropPercent = ((rollingAverage - activityIndex) / rollingAverage) * 100.0;
            if (dropPercent > 40) {
                riskLevel = "High";
            } else if (dropPercent > 20) {
                riskLevel = "Medium";
            }
        }

        WeeklyMetric metric = weeklyMetricCatalogRepository.findByProjectAndWeekStart(projectId, weekStart)
                .orElse(WeeklyMetric.create(projectId, weekStart, OffsetDateTime.now(ZoneOffset.UTC)));
        metric.updateSummary(progressDelta, activityIndex, totalHours, riskLevel);
        weeklyMetricCatalogRepository.save(metric);
        return true;
    }

    private void generateAlerts(Integer projectId) {
        List<DailyMetric> daily = dailyMetricCatalogRepository.findSince(projectId, LocalDate.now(ZoneOffset.UTC).minusDays(14));
        List<WeeklyMetric> weekly = weeklyMetricCatalogRepository.findLatest(projectId, 4);

        long consecutiveLow = daily.reversed().stream()
                .takeWhile(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount())
                        + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()) <= 2)
                .count();
        if (consecutiveLow >= 3) {
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

        long consecutiveNegativeWeeks = weekly.stream()
                .takeWhile(row -> row.getProgressDelta() != null && row.getProgressDelta() < 0)
                .count();
        if (consecutiveNegativeWeeks >= 2) {
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

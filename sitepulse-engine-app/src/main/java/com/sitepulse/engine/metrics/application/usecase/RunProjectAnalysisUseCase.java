package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.application.enums.ImageEvidenceCandidateTag;
import com.sitepulse.engine.detection.domain.model.ImageEvidenceSummary;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.alert.application.command.CreateAlertCommand;
import com.sitepulse.engine.alert.application.usecase.AutoResolveAlertsUseCase;
import com.sitepulse.engine.alert.application.usecase.CreateAlertUseCase;
import com.sitepulse.engine.alert.domain.enums.AlertSeverity;
import com.sitepulse.engine.metrics.application.result.AnalysisRunResult;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityReasonCode;
import com.sitepulse.engine.metrics.domain.model.DailyActivityAssessment;
import com.sitepulse.engine.metrics.domain.model.DailyActivityEvidence;
import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.model.DetectionActivitySample;
import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import com.sitepulse.engine.metrics.domain.port.DetectionMetricsReadModel;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import com.sitepulse.engine.metrics.domain.policy.DeclineDetectionPolicy;
import com.sitepulse.engine.metrics.domain.policy.StallDetectionPolicy;
import com.sitepulse.engine.metrics.domain.service.DailyActiveHoursCalculator;
import com.sitepulse.engine.metrics.domain.service.DailyActivityClassificationService;
import com.sitepulse.engine.metrics.domain.service.DailyActivityAggregator;
import com.sitepulse.engine.metrics.domain.service.WeeklyRollupCalculator;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final ProcessedImageReadModel processedImageReadModel;
    private final CreateAlertUseCase createAlertUseCase;
    private final AutoResolveAlertsUseCase autoResolveAlertsUseCase;
    private final JsonUtils jsonUtils;

    private final DailyActivityAggregator dailyAggregator = new DailyActivityAggregator();
    private final DailyActiveHoursCalculator dailyActiveHoursCalculator = new DailyActiveHoursCalculator();
    private final DailyActivityClassificationService dailyActivityClassificationService = new DailyActivityClassificationService();
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
        DailyActivityEvidence evidence = buildDailyEvidence(projectId, targetDate, aggregation);
        DailyActivityAssessment assessment = dailyActivityClassificationService.classify(evidence);
        DailyMetric metric = dailyMetricCatalogRepository.findByProjectAndDate(projectId, targetDate)
                .orElse(DailyMetric.create(projectId, targetDate, OffsetDateTime.now(ZoneOffset.UTC)));
        metric.updateCounts(aggregation.peopleCount(), aggregation.vehicleCount(), evidence.activeHours());
        metric.updateAssessment(
                assessment.activityStatus(),
                assessment.activityConfidence(),
                assessment.weatherStatus(),
                assessment.weatherImpacted(),
                assessment.reasonCodes().stream().map(DailyActivityReasonCode::toPersistenceValue).toList(),
                assessment.summaryNote()
        );
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

    private DailyActivityEvidence buildDailyEvidence(
            Integer projectId,
            LocalDate targetDate,
            DailyActivityAggregator.DailyAggregation aggregation
    ) {
        OffsetDateTime from = targetDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = targetDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<StoredImage> images = processedImageReadModel.findDoneInRange(projectId, from, to);

        double maxActivityScore = 0.0;
        double maxChangeScore = 0.0;
        double qualityScoreSum = 0.0;
        int qualityScoreCount = 0;
        int movementSignalCount = 0;
        boolean loadingActivityDetected = false;
        boolean equipmentChangeDetected = false;
        boolean rainObserved = false;
        boolean snowObserved = false;
        boolean limitedVisibility = false;
        Set<Integer> activeHours = new HashSet<>();

        for (StoredImage image : images) {
            maxActivityScore = Math.max(maxActivityScore, safeScore(image.getEvidenceActivityScore()));
            maxChangeScore = Math.max(maxChangeScore, safeScore(image.getEvidenceChangeScore()));
            if (image.getEvidenceQualityScore() != null) {
                qualityScoreSum += image.getEvidenceQualityScore();
                qualityScoreCount++;
            }
            if (containsRain(image.getWeatherNote())) {
                rainObserved = true;
            }
            if (containsSnow(image.getWeatherNote())) {
                snowObserved = true;
            }

            ImageEvidenceSummary summary = readSummary(image.getEvidenceSummary());
            List<String> candidateTags = summary.candidateTags();
            List<String> firstAppearanceFlags = summary.firstAppearanceFlags();
            List<String> changeFlags = summary.changeFlags();
            List<String> qualityFlags = summary.qualityFlags();

            movementSignalCount += candidateTags.size() + firstAppearanceFlags.size() + changeFlags.size();
            loadingActivityDetected |= candidateTags.contains(ImageEvidenceCandidateTag.LOADING_ACTIVITY.value());
            equipmentChangeDetected |= candidateTags.contains(ImageEvidenceCandidateTag.EQUIPMENT_CHANGE.value())
                    || !firstAppearanceFlags.isEmpty()
                    || !changeFlags.isEmpty();
            limitedVisibility |= !qualityFlags.isEmpty();
            if (image.getCapturedAt() != null && dailyActiveHoursCalculator.isActiveImage(summary)) {
                activeHours.add(image.getCapturedAt().getHour());
            }
        }

        return new DailyActivityEvidence(
                images.size(),
                aggregation.peopleCount(),
                aggregation.vehicleCount(),
                activeHours.size(),
                maxActivityScore,
                maxChangeScore,
                qualityScoreCount == 0 ? 0.0 : qualityScoreSum / qualityScoreCount,
                movementSignalCount,
                loadingActivityDetected,
                equipmentChangeDetected,
                rainObserved,
                snowObserved,
                limitedVisibility
        );
    }

    private ImageEvidenceSummary readSummary(String evidenceSummary) {
        if (evidenceSummary == null || evidenceSummary.isBlank()) {
            return ImageEvidenceSummary.empty();
        }
        return jsonUtils.read(evidenceSummary, ImageEvidenceSummary.class);
    }

    private boolean containsRain(String weatherNote) {
        if (weatherNote == null || weatherNote.isBlank()) {
            return false;
        }
        String normalized = weatherNote.toLowerCase();
        return normalized.contains("rain") || normalized.contains("drizzle") || normalized.contains("shower");
    }

    private boolean containsSnow(String weatherNote) {
        if (weatherNote == null || weatherNote.isBlank()) {
            return false;
        }
        String normalized = weatherNote.toLowerCase();
        return normalized.contains("snow") || normalized.contains("ice");
    }

    private double safeScore(Double value) {
        return value == null ? 0.0 : value;
    }
}

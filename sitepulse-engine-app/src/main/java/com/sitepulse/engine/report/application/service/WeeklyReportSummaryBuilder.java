package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class WeeklyReportSummaryBuilder {

    private final DailyReportSummaryBuilder dailyReportSummaryBuilder;
    private final WeeklyMetricCatalogRepository weeklyMetricCatalogRepository;

    public WeeklyReportSummaryBuilder(
            DailyReportSummaryBuilder dailyReportSummaryBuilder,
            WeeklyMetricCatalogRepository weeklyMetricCatalogRepository
    ) {
        this.dailyReportSummaryBuilder = dailyReportSummaryBuilder;
        this.weeklyMetricCatalogRepository = weeklyMetricCatalogRepository;
    }

    public WeeklyReportSummary build(
            Integer projectId,
            LocalDate weekStart,
            Map<LocalDate, OffsetDateTime> fromByDay,
            Map<LocalDate, OffsetDateTime> toByDay
    ) {
        List<DailyReportSummary> dailySummaries = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            LocalDate day = weekStart.plusDays(index);
            dailySummaries.add(dailyReportSummaryBuilder.build(projectId, day, fromByDay.get(day), toByDay.get(day)));
        }
        int imageCount = dailySummaries.stream().mapToInt(DailyReportSummary::imageCount).sum();
        int activeDays = (int) dailySummaries.stream().filter(summary -> summary.imageCount() > 0).count();
        Map<WeatherSummary, Long> weatherCounts = dailySummaries.stream()
                .filter(summary -> summary.imageCount() > 0)
                .collect(Collectors.groupingBy(DailyReportSummary::weatherSummary, Collectors.counting()));
        WeatherSummary weatherPattern = resolveWeatherPattern(weatherCounts);
        WeeklyMetric metric = weeklyMetricCatalogRepository.findByProjectAndWeekStart(projectId, weekStart).orElse(null);
        List<String> notableEvents = dailySummaries.stream()
                .flatMap(summary -> summary.notableEvents().stream())
                .distinct()
                .limit(8)
                .toList();
        ConfidenceLevel confidence = confidence(activeDays, imageCount, metric != null);
        String context = buildContext(weekStart, dailySummaries, weatherPattern, notableEvents, confidence, metric);
        return new WeeklyReportSummary(weekStart, imageCount, activeDays, weatherPattern, confidence, notableEvents, context);
    }

    private String buildContext(
            LocalDate weekStart,
            List<DailyReportSummary> dailySummaries,
            WeatherSummary weatherPattern,
            List<String> notableEvents,
            ConfidenceLevel confidence,
            WeeklyMetric metric
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("### Structured Weekly Summary\n");
        builder.append("- week_start: ").append(weekStart).append('\n');
        builder.append("- days_with_activity: ").append(dailySummaries.stream().filter(summary -> summary.imageCount() > 0).count()).append('\n');
        builder.append("- images_analyzed: ").append(dailySummaries.stream().mapToInt(DailyReportSummary::imageCount).sum()).append('\n');
        builder.append("- weather_pattern: ").append(weatherPattern.toPersistenceValue()).append('\n');
        builder.append("- report_confidence: ").append(confidence.toPersistenceValue()).append('\n');
        if (metric != null) {
            builder.append("- weekly_metrics: progress_delta=").append(metric.getProgressDelta())
                    .append(", activity_index=").append(metric.getActivityIndex())
                    .append(", active_hours=").append(metric.getActiveHours())
                    .append(", risk=").append(metric.getRiskLevel().toPersistenceValue())
                    .append('\n');
        }
        builder.append("- daily_rollup:\n");
        dailySummaries.stream()
                .filter(summary -> summary.imageCount() > 0)
                .forEach(summary -> builder.append("  - ").append(summary.date())
                        .append(": images=").append(summary.imageCount())
                        .append(", weather=").append(summary.weatherSummary().toPersistenceValue())
                        .append(", confidence=").append(summary.confidenceLevel().toPersistenceValue())
                        .append('\n'));
        if (!notableEvents.isEmpty()) {
            builder.append("- notable_events:\n");
            notableEvents.forEach(event -> builder.append("  - ").append(event).append('\n'));
        }
        return builder.toString();
    }

    private ConfidenceLevel confidence(int activeDays, int imageCount, boolean hasWeeklyMetric) {
        if (activeDays >= 4 && imageCount >= 12 && hasWeeklyMetric) {
            return ConfidenceLevel.HIGH;
        }
        if (activeDays >= 2 && imageCount >= 4) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }

    private WeatherSummary resolveWeatherPattern(Map<WeatherSummary, Long> weatherCounts) {
        if (weatherCounts.isEmpty()) {
            return WeatherSummary.UNCLEAR;
        }
        long nonUnclearCategories = weatherCounts.entrySet().stream()
                .filter(entry -> entry.getKey() != WeatherSummary.UNCLEAR)
                .count();
        if (nonUnclearCategories > 1) {
            return WeatherSummary.MIXED;
        }
        return weatherCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(WeatherSummary.UNCLEAR);
    }
}

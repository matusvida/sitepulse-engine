package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.application.enums.ImageEvidenceSummaryField;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DailyReportSummaryBuilder {

    private final ProcessedImageReadModel processedImageReadModel;
    private final DailyMetricCatalogRepository dailyMetricCatalogRepository;
    private final JsonUtils jsonUtils;

    public DailyReportSummaryBuilder(
            ProcessedImageReadModel processedImageReadModel,
            DailyMetricCatalogRepository dailyMetricCatalogRepository,
            JsonUtils jsonUtils
    ) {
        this.processedImageReadModel = processedImageReadModel;
        this.dailyMetricCatalogRepository = dailyMetricCatalogRepository;
        this.jsonUtils = jsonUtils;
    }

    public DailyReportSummary build(Integer projectId, LocalDate date, OffsetDateTime fromUtc, OffsetDateTime toUtc) {
        List<StoredImage> images = processedImageReadModel.findDoneInRange(projectId, fromUtc, toUtc);
        Map<WeatherSummary, Long> weatherCounts = images.stream()
                .map(image -> WeatherSummary.fromObservation(image.getWeatherNote()))
                .collect(Collectors.groupingBy(val -> val, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> classCounts = new LinkedHashMap<>();
        List<String> notableEvents = new ArrayList<>();
        for (StoredImage image : images) {
            processedImageReadModel.findDetections(image.getId()).forEach(detection ->
                    classCounts.merge(detection.className().toLowerCase(Locale.ROOT), 1L, Long::sum));
            notableEvents.addAll(extractEvents(image.getEvidenceSummary()));
        }
        List<String> dominantClasses = classCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();
        DailyMetric metric = dailyMetricCatalogRepository.findByProjectAndDate(projectId, date).orElse(null);
        ConfidenceLevel confidence = confidence(images.size());
        WeatherSummary weatherSummary = resolveWeatherSummary(weatherCounts);
        DailyActivityStatus activityStatus = metric == null || metric.getActivityStatus() == null
                ? DailyActivityStatus.UNKNOWN
                : metric.getActivityStatus();
        String context = buildContext(date, images.size(), weatherSummary, confidence, dominantClasses, dedupe(notableEvents), metric);
        return new DailyReportSummary(
                date,
                images.size(),
                activityStatus,
                weatherSummary,
                confidence,
                dominantClasses,
                dedupe(notableEvents),
                context
        );
    }

    private String buildContext(
            LocalDate date,
            int imageCount,
            WeatherSummary weatherSummary,
            ConfidenceLevel confidence,
            List<String> dominantClasses,
            List<String> notableEvents,
            DailyMetric metric
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("### Structured Daily Summary\n");
        builder.append("- date: ").append(date).append('\n');
        builder.append("- images_analyzed: ").append(imageCount).append('\n');
        builder.append("- weather_summary: ").append(weatherSummary.toPersistenceValue()).append('\n');
        builder.append("- report_confidence: ").append(confidence.toPersistenceValue()).append('\n');
        if (metric != null) {
            builder.append("- daily_metrics: people=").append(metric.getPeopleCount())
                    .append(", vehicles=").append(metric.getVehicleCount())
                    .append(", active_hours=").append(metric.getActiveHours())
                    .append(", activity_status=").append(metric.getActivityStatus() == null ? "unknown" : metric.getActivityStatus().toPersistenceValue())
                    .append(", activity_confidence=").append(metric.getActivityConfidence() == null ? "low" : metric.getActivityConfidence().toPersistenceValue())
                    .append(", weather_status=").append(metric.getWeatherStatus() == null ? "unclear" : metric.getWeatherStatus().toPersistenceValue())
                    .append(", weather_impacted=").append(Boolean.TRUE.equals(metric.getWeatherImpacted()))
                    .append('\n');
        }
        if (!dominantClasses.isEmpty()) {
            builder.append("- dominant_classes: ").append(String.join(", ", dominantClasses)).append('\n');
        }
        if (!notableEvents.isEmpty()) {
            builder.append("- notable_events:\n");
            notableEvents.forEach(event -> builder.append("  - ").append(event).append('\n'));
        }
        return builder.toString();
    }

    private List<String> extractEvents(String evidenceSummary) {
        if (evidenceSummary == null || evidenceSummary.isBlank()) {
            return List.of();
        }
        Map<String, Object> map = jsonUtils.readMap(evidenceSummary);
        List<String> events = new ArrayList<>();
        events.addAll(tagEvents(map.get(ImageEvidenceSummaryField.CANDIDATE_TAGS.key())));
        events.addAll(tagEvents(map.get(ImageEvidenceSummaryField.FIRST_APPEARANCE_FLAGS.key())));
        return events;
    }

    @SuppressWarnings("unchecked")
    private List<String> tagEvents(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::humanize)
                .toList();
    }

    private String humanize(String value) {
        return value.replace('_', ' ');
    }

    private List<String> dedupe(List<String> values) {
        return values.stream().distinct().limit(6).toList();
    }

    private ConfidenceLevel confidence(int imageCount) {
        if (imageCount >= 6) {
            return ConfidenceLevel.HIGH;
        }
        if (imageCount >= 3) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }

    private WeatherSummary resolveWeatherSummary(Map<WeatherSummary, Long> weatherCounts) {
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

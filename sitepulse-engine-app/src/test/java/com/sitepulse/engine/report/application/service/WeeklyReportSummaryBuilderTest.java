package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.RiskLevel;
import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyReportSummaryBuilderTest {

    @Test
    void buildIncludesPerDayClassesAndEventsForWeeklyProgressReasoning() {
        DailyReportSummaryBuilder dailyBuilder = new DailyReportSummaryBuilder(null, null, null) {
            @Override
            public DailyReportSummary build(Integer projectId, LocalDate date, OffsetDateTime fromUtc, OffsetDateTime toUtc) {
                if (date.equals(LocalDate.of(2026, 4, 14))) {
                    return new DailyReportSummary(
                            date,
                            3,
                            DailyActivityStatus.ACTIVE,
                            WeatherSummary.OVERCAST,
                            ConfidenceLevel.MEDIUM,
                            List.of("truck", "concrete_mixer_truck"),
                            List.of("increase in concrete mixer truck presence"),
                            "day one"
                    );
                }
                if (date.equals(LocalDate.of(2026, 4, 16))) {
                    return new DailyReportSummary(
                            date,
                            4,
                            DailyActivityStatus.ACTIVE,
                            WeatherSummary.CLEAR,
                            ConfidenceLevel.HIGH,
                            List.of("crane_tower", "truck"),
                            List.of("first appearance of crane tower"),
                            "day three"
                    );
                }
                return new DailyReportSummary(
                        date,
                        0,
                        DailyActivityStatus.UNKNOWN,
                        WeatherSummary.UNCLEAR,
                        ConfidenceLevel.LOW,
                        List.of(),
                        List.of(),
                        "empty"
                );
            }
        };
        WeeklyReportSummaryBuilder builder = new WeeklyReportSummaryBuilder(
                dailyBuilder,
                new WeeklyMetricCatalogRepository() {
                    @Override
                    public Optional<WeeklyMetric> findByProjectAndWeekStart(Integer projectId, LocalDate weekStart) {
                        return Optional.of(WeeklyMetric.restore(
                                1,
                                projectId,
                                weekStart,
                                new BigDecimal("0.6"),
                                new BigDecimal("0.8"),
                                14.0,
                                RiskLevel.LOW,
                                OffsetDateTime.of(2026, 4, 21, 10, 0, 0, 0, ZoneOffset.UTC)
                        ));
                    }

                    @Override
                    public WeeklyMetric save(WeeklyMetric metric) {
                        return metric;
                    }

                    @Override
                    public List<WeeklyMetric> findLatest(Integer projectId, int limit) {
                        return List.of();
                    }

                    @Override
                    public BigDecimal findAverageActivityBefore(Integer projectId, LocalDate weekStart) {
                        return BigDecimal.ZERO;
                    }
                }
        );

        Map<LocalDate, OffsetDateTime> fromByDay = new LinkedHashMap<>();
        Map<LocalDate, OffsetDateTime> toByDay = new LinkedHashMap<>();
        LocalDate weekStart = LocalDate.of(2026, 4, 14);
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            fromByDay.put(day, day.atStartOfDay().atOffset(ZoneOffset.UTC));
            toByDay.put(day, day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        }

        WeeklyReportSummary summary = builder.build(1, weekStart, weekStart.plusDays(6), fromByDay, toByDay);

        assertTrue(summary.contextText().contains("dominant_classes=truck, concrete_mixer_truck"));
        assertTrue(summary.contextText().contains("notable_events=increase in concrete mixer truck presence"));
        assertTrue(summary.contextText().contains("dominant_classes=crane_tower, truck"));
        assertTrue(summary.contextText().contains("notable_events=first appearance of crane tower"));
    }
}

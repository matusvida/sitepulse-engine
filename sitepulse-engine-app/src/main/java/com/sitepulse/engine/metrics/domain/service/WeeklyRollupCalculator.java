package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.enums.RiskLevel;
import com.sitepulse.engine.metrics.domain.policy.RiskClassificationPolicy;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

public class WeeklyRollupCalculator {

    private final RiskClassificationPolicy riskPolicy = new RiskClassificationPolicy();

    public record WeeklyRollup(double progressDelta, double activityIndex, double activeHours, RiskLevel riskLevel) {}

    public WeeklyRollup calculate(
            List<DailyMetric> currentWeekDaily,
            List<DailyMetric> previousWeekDaily,
            List<DailyMetric> allHistoricDaily,
            Double rollingAverageActivity
    ) {
        double totalActivity = sumActivity(currentWeekDaily);
        double totalHours = currentWeekDaily.stream()
                .mapToDouble(row -> row.getActiveHours() == null ? 0 : row.getActiveHours())
                .sum();
        double previousActivity = sumActivity(previousWeekDaily);

        double progressDelta = previousActivity > 0
                ? ((totalActivity - previousActivity) / previousActivity) * 100.0
                : (totalActivity > 0 ? 100.0 : 0.0);

        double maxActivity = allHistoricDaily.stream()
                .collect(Collectors.groupingBy(row -> row.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
                .values().stream()
                .mapToDouble(this::sumActivity)
                .max()
                .orElse(1.0);

        double activityIndex = Math.clamp(maxActivity == 0 ? 0 : (totalActivity / maxActivity) * 100.0, 0.0, 100.0);
        RiskLevel riskLevel = riskPolicy.classify(activityIndex, rollingAverageActivity);

        return new WeeklyRollup(progressDelta, activityIndex, totalHours, riskLevel);
    }

    private double sumActivity(List<DailyMetric> daily) {
        return daily.stream()
                .mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount())
                        + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()))
                .sum();
    }
}

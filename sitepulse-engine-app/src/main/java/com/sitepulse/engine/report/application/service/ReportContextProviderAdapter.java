package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.metrics.application.result.DailyMetricResult;
import com.sitepulse.engine.metrics.application.result.WeeklyMetricResult;
import com.sitepulse.engine.metrics.application.usecase.ListDailyMetricsQuery;
import com.sitepulse.engine.metrics.application.usecase.ListWeeklyMetricsQuery;
import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.application.usecase.GetLatestPlanQuery;
import com.sitepulse.engine.report.domain.port.ReportContextProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportContextProviderAdapter implements ReportContextProvider {

    private final ListDailyMetricsQuery listDailyMetricsQuery;
    private final ListWeeklyMetricsQuery listWeeklyMetricsQuery;
    private final GetLatestPlanQuery getLatestPlanQuery;

    @Override
    public String getMetricsSummary(Integer projectId, int days) {
        StringBuilder builder = new StringBuilder();
        builder.append("### Daily Metrics\n");
        List<DailyMetricResult> dailyMetrics = listDailyMetricsQuery.list(projectId, days);
        int activeDays = 0;
        int inactiveDays = 0;
        int unknownDays = 0;
        int weatherImpactedDays = 0;
        int rainDays = 0;
        int snowDays = 0;
        dailyMetrics.forEach(row -> builder.append("- ").append(row.getDate())
                .append(": people=").append(row.getPeopleCount())
                .append(", vehicles=").append(row.getVehicleCount())
                .append(", active_hours=").append(row.getActiveHours())
                .append(", activity_status=").append(row.getActivityStatus())
                .append(", weather_status=").append(row.getWeatherStatus())
                .append(", weather_impacted=").append(row.isWeatherImpacted())
                .append('\n'));
        for (DailyMetricResult row : dailyMetrics) {
            if ("active".equals(row.getActivityStatus())) {
                activeDays++;
            } else if ("inactive".equals(row.getActivityStatus())) {
                inactiveDays++;
            } else {
                unknownDays++;
            }
            if (row.isWeatherImpacted()) {
                weatherImpactedDays++;
            }
            if ("rain".equals(row.getWeatherStatus())) {
                rainDays++;
            } else if ("snow".equals(row.getWeatherStatus())) {
                snowDays++;
            }
        }
        builder.append("- summary: active_days=").append(activeDays)
                .append(", inactive_days=").append(inactiveDays)
                .append(", unknown_days=").append(unknownDays)
                .append(", weather_impacted_days=").append(weatherImpactedDays)
                .append(", rain_days=").append(rainDays)
                .append(", snow_days=").append(snowDays)
                .append('\n');

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

    @Override
    public String getMilestoneSummary(Integer projectId) {
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

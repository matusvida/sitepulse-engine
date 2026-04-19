package com.sitepulse.engine.metrics.web;

import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.metrics.api.MetricsApi;
import com.sitepulse.engine.http.metrics.dto.ActivityHeatmapPointView;
import com.sitepulse.engine.http.metrics.dto.ActivitySummaryView;
import com.sitepulse.engine.http.metrics.dto.DailyMetricView;
import com.sitepulse.engine.http.metrics.dto.MetricsGenerateRequest;
import com.sitepulse.engine.http.metrics.dto.WeeklyMetricView;
import com.sitepulse.engine.metrics.application.result.ActivityHeatmapPointResult;
import com.sitepulse.engine.metrics.application.result.ActivitySummaryResult;
import com.sitepulse.engine.metrics.application.result.DailyMetricResult;
import com.sitepulse.engine.metrics.application.result.WeeklyMetricResult;
import com.sitepulse.engine.metrics.application.usecase.GetActivityHeatmapQuery;
import com.sitepulse.engine.metrics.application.usecase.GetActivitySummaryQuery;
import com.sitepulse.engine.metrics.application.usecase.ListDailyMetricsQuery;
import com.sitepulse.engine.metrics.application.usecase.ListWeeklyMetricsQuery;
import com.sitepulse.engine.metrics.application.usecase.RunProjectAnalysisUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MetricsController implements MetricsApi {

    private final ListDailyMetricsQuery listDailyMetricsQuery;
    private final ListWeeklyMetricsQuery listWeeklyMetricsQuery;
    private final RunProjectAnalysisUseCase runProjectAnalysisUseCase;
    private final GetActivityHeatmapQuery getActivityHeatmapQuery;
    private final GetActivitySummaryQuery getActivitySummaryQuery;

    @Override
    public List<DailyMetricView> dailyMetrics(Integer projectId, int days) {
        return listDailyMetricsQuery.list(projectId, days).stream()
                .map(this::toDailyMetricView)
                .toList();
    }

    @Override
    public List<WeeklyMetricView> weeklyMetrics(Integer projectId, int weeks) {
        return listWeeklyMetricsQuery.list(projectId, weeks).stream()
                .map(this::toWeeklyMetricView)
                .toList();
    }

    @Override
    public ActionResponse generateMetrics(Integer projectId, MetricsGenerateRequest request) {
        runProjectAnalysisUseCase.run(projectId, request == null || request.getLookbackDays() == null ? 30 : request.getLookbackDays());
        return new ActionResponse("accepted", "Metrics generation started", projectId);
    }

    @Override
    public List<ActivityHeatmapPointView> activityHeatmap(Integer projectId) {
        return getActivityHeatmapQuery.get(projectId).stream()
                .map(this::toActivityHeatmapView)
                .toList();
    }

    @Override
    public ActivitySummaryView activitySummary(Integer projectId, int days) {
        return toActivitySummaryView(getActivitySummaryQuery.get(projectId, days));
    }

    private DailyMetricView toDailyMetricView(DailyMetricResult row) {
        return new DailyMetricView(
                row.getDate().toString(),
                row.getPeopleCount(),
                row.getVehicleCount(),
                row.getActiveHours(),
                row.getActivityStatus(),
                row.getActivityConfidence(),
                row.getWeatherStatus(),
                row.isWeatherImpacted(),
                row.getReasonCodes(),
                row.getSummaryNote()
        );
    }

    private WeeklyMetricView toWeeklyMetricView(WeeklyMetricResult row) {
        return new WeeklyMetricView(
                row.getWeekStart().toString(),
                row.getProgressDelta(),
                row.getActivityIndex(),
                row.getActiveHours(),
                row.getRiskLevel()
        );
    }

    private ActivityHeatmapPointView toActivityHeatmapView(ActivityHeatmapPointResult row) {
        return new ActivityHeatmapPointView(row.getDayOfWeek(), row.getHour(), row.getCount());
    }

    private ActivitySummaryView toActivitySummaryView(ActivitySummaryResult row) {
        return new ActivitySummaryView(
                row.getTotalDays(),
                row.getActiveDays(),
                row.getInactiveDays(),
                row.getUnknownDays(),
                row.getWeatherImpactedDays(),
                row.getRainDays(),
                row.getSnowDays()
        );
    }
}

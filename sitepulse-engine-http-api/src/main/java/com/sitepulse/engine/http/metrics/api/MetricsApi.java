package com.sitepulse.engine.http.metrics.api;

import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.metrics.dto.ActivityHeatmapPointView;
import com.sitepulse.engine.http.metrics.dto.ActivitySummaryView;
import com.sitepulse.engine.http.metrics.dto.DailyMetricView;
import com.sitepulse.engine.http.metrics.dto.MetricsGenerateRequest;
import com.sitepulse.engine.http.metrics.dto.WeeklyMetricView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Metrics")
@RequestMapping("/api")
public interface MetricsApi {

    @Operation(summary = "Get daily metrics")
    @GetMapping("/projects/{projectId}/metrics/daily")
    List<DailyMetricView> dailyMetrics(@PathVariable Integer projectId, @RequestParam(defaultValue = "28") int days);

    @Operation(summary = "Get weekly metrics")
    @GetMapping("/projects/{projectId}/metrics/weekly")
    List<WeeklyMetricView> weeklyMetrics(@PathVariable Integer projectId, @RequestParam(defaultValue = "12") int weeks);

    @Operation(summary = "Trigger metrics generation")
    @PostMapping("/projects/{projectId}/metrics/generate")
    ActionResponse generateMetrics(@PathVariable Integer projectId, @RequestBody(required = false) MetricsGenerateRequest request);

    @Operation(summary = "Get activity heatmap")
    @GetMapping("/projects/{projectId}/activity/heatmap")
    List<ActivityHeatmapPointView> activityHeatmap(@PathVariable Integer projectId);

    @Operation(summary = "Get activity summary")
    @GetMapping("/projects/{projectId}/activity/summary")
    ActivitySummaryView activitySummary(@PathVariable Integer projectId, @RequestParam(defaultValue = "28") int days);
}

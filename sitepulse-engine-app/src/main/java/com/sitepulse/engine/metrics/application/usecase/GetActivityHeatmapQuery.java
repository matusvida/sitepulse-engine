package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.metrics.application.result.ActivityHeatmapPointResult;
import com.sitepulse.engine.metrics.domain.port.DetectionMetricsReadModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetActivityHeatmapQuery {

    private final DetectionMetricsReadModel detectionMetricsReadModel;

    public List<ActivityHeatmapPointResult> get(Integer projectId) {
        return detectionMetricsReadModel.getActivityHeatmap(projectId).stream()
                .map(row -> new ActivityHeatmapPointResult(row.getDayOfWeek(), row.getHour(), row.getCount()))
                .toList();
    }
}

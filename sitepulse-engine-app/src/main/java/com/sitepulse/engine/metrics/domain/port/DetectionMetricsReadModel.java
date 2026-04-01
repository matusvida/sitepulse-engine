package com.sitepulse.engine.metrics.domain.port;

import com.sitepulse.engine.metrics.domain.model.ActivityHeatmapPoint;
import com.sitepulse.engine.metrics.domain.model.DetectionActivitySample;
import java.time.LocalDate;
import java.util.List;

public interface DetectionMetricsReadModel {

    List<LocalDate> findProcessedDates(Integer projectId, LocalDate cutoff);

    List<LocalDate> findCompletedWeeks(Integer projectId, LocalDate sinceDate);

    List<DetectionActivitySample> findDetectionActivityForDay(Integer projectId, LocalDate targetDate);

    List<ActivityHeatmapPoint> getActivityHeatmap(Integer projectId);
}

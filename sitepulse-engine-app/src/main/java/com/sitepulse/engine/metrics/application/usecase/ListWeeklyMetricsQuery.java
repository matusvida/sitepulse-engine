package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.metrics.application.result.WeeklyMetricResult;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListWeeklyMetricsQuery {

    private final WeeklyMetricCatalogRepository weeklyMetricCatalogRepository;

    public List<WeeklyMetricResult> list(Integer projectId, int weeks) {
        List<WeeklyMetricResult> results = new ArrayList<>(weeklyMetricCatalogRepository.findLatest(projectId, weeks).stream()
                .map(row -> new WeeklyMetricResult(
                        row.getWeekStart(),
                        row.getProgressDelta() == null ? 0.0 : row.getProgressDelta(),
                        row.getActivityIndex() == null ? 0.0 : row.getActivityIndex(),
                        row.getActiveHours() == null ? 0.0 : row.getActiveHours(),
                        row.getRiskLevel() == null ? "Low" : row.getRiskLevel().toPersistenceValue()
                ))
                .toList());
        Collections.reverse(results);
        return results;
    }
}

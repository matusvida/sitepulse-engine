package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.metrics.application.result.DailyMetricResult;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListDailyMetricsQuery {

    private final DailyMetricCatalogRepository dailyMetricCatalogRepository;

    public List<DailyMetricResult> list(Integer projectId, int days) {
        return dailyMetricCatalogRepository.findSince(projectId, LocalDate.now(ZoneOffset.UTC).minusDays(days)).stream()
                .map(row -> new DailyMetricResult(
                        row.getDate(),
                        row.getPeopleCount() == null ? 0 : row.getPeopleCount(),
                        row.getVehicleCount() == null ? 0 : row.getVehicleCount(),
                        row.getActiveHours() == null ? 0.0 : row.getActiveHours(),
                        row.getActivityStatus() == null ? "unknown" : row.getActivityStatus().toPersistenceValue(),
                        row.getActivityConfidence() == null ? "low" : row.getActivityConfidence().toPersistenceValue(),
                        row.getWeatherStatus() == null ? "unclear" : row.getWeatherStatus().toPersistenceValue(),
                        Boolean.TRUE.equals(row.getWeatherImpacted()),
                        row.getReasonCodes() == null ? List.of() : row.getReasonCodes(),
                        row.getSummaryNote()
                ))
                .toList();
    }
}

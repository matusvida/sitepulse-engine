package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.metrics.application.result.ActivitySummaryResult;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.DailyWeatherStatus;
import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetActivitySummaryQuery {

    private final DailyMetricCatalogRepository dailyMetricCatalogRepository;

    public ActivitySummaryResult get(Integer projectId, int days) {
        List<DailyMetric> metrics = dailyMetricCatalogRepository.findSince(projectId, LocalDate.now(ZoneOffset.UTC).minusDays(days));
        int activeDays = (int) metrics.stream().filter(metric -> metric.getActivityStatus() == DailyActivityStatus.ACTIVE).count();
        int inactiveDays = (int) metrics.stream().filter(metric -> metric.getActivityStatus() == DailyActivityStatus.INACTIVE).count();
        int unknownDays = (int) metrics.stream().filter(metric -> metric.getActivityStatus() == DailyActivityStatus.UNKNOWN).count();
        int weatherImpactedDays = (int) metrics.stream().filter(metric -> Boolean.TRUE.equals(metric.getWeatherImpacted())).count();
        int rainDays = (int) metrics.stream().filter(metric -> metric.getWeatherStatus() == DailyWeatherStatus.RAIN).count();
        int snowDays = (int) metrics.stream().filter(metric -> metric.getWeatherStatus() == DailyWeatherStatus.SNOW).count();
        return new ActivitySummaryResult(metrics.size(), activeDays, inactiveDays, unknownDays, weatherImpactedDays, rainDays, snowDays);
    }
}

package com.sitepulse.engine.metrics.domain.port;

import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyMetricCatalogRepository {

    Optional<WeeklyMetric> findByProjectAndWeekStart(Integer projectId, LocalDate weekStart);

    WeeklyMetric save(WeeklyMetric metric);

    List<WeeklyMetric> findLatest(Integer projectId, int limit);

    BigDecimal findAverageActivityBefore(Integer projectId, LocalDate weekStart);
}

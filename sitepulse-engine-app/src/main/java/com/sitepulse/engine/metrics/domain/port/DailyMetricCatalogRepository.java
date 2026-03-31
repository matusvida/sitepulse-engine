package com.sitepulse.engine.metrics.domain.port;

import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMetricCatalogRepository {

    Optional<DailyMetric> findByProjectAndDate(Integer projectId, LocalDate date);

    DailyMetric save(DailyMetric metric);

    List<DailyMetric> findSince(Integer projectId, LocalDate sinceDate);

    List<DailyMetric> findBetween(Integer projectId, LocalDate from, LocalDate to);

    List<DailyMetric> findAllSince(Integer projectId, LocalDate sinceDate);
}

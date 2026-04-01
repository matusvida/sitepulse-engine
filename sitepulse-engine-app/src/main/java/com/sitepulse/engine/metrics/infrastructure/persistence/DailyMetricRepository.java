package com.sitepulse.engine.metrics.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyMetricRepository extends JpaRepository<DailyMetricEntity, Integer> {

    Optional<DailyMetricEntity> findByProjectIdAndDate(Integer projectId, LocalDate date);

    List<DailyMetricEntity> findByProjectIdAndDateGreaterThanEqualOrderByDateAsc(Integer projectId, LocalDate cutoff);

    List<DailyMetricEntity> findByProjectIdAndDateBetweenOrderByDateAsc(Integer projectId, LocalDate from, LocalDate to);
}

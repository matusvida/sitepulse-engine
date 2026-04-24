package com.sitepulse.engine.metrics.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeeklyMetricRepository extends JpaRepository<WeeklyMetricEntity, Integer> {

    Optional<WeeklyMetricEntity> findByProjectIdAndWeekStart(Integer projectId, LocalDate weekStart);

    List<WeeklyMetricEntity> findByProjectIdOrderByWeekStartDesc(Integer projectId, Pageable pageable);

    List<WeeklyMetricEntity> findByProjectIdAndWeekStartBetweenOrderByWeekStartAsc(Integer projectId, LocalDate from, LocalDate to);

    @Query("""
            select avg(w.activityIndex) from WeeklyMetricEntity w
            where w.projectId = :projectId and w.weekStart < :beforeWeek
            """)
    BigDecimal findAverageActivityBefore(@Param("projectId") Integer projectId, @Param("beforeWeek") LocalDate beforeWeek);
}

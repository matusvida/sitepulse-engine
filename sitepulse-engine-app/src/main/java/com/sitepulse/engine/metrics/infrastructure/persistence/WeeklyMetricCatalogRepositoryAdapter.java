package com.sitepulse.engine.metrics.infrastructure.persistence;

import com.sitepulse.engine.metrics.domain.enums.RiskLevel;
import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WeeklyMetricCatalogRepositoryAdapter implements WeeklyMetricCatalogRepository {

    private final WeeklyMetricRepository weeklyMetricRepository;

    @Override
    public Optional<WeeklyMetric> findByProjectAndWeekStart(Integer projectId, LocalDate weekStart) {
        return weeklyMetricRepository.findByProjectIdAndWeekStart(projectId, weekStart).map(this::toDomain);
    }

    @Override
    public WeeklyMetric save(WeeklyMetric metric) {
        WeeklyMetricEntity entity = metric.getId() == null
                ? new WeeklyMetricEntity()
                : weeklyMetricRepository.findById(metric.getId()).orElseGet(WeeklyMetricEntity::new);
        entity.setProjectId(metric.getProjectId());
        entity.setWeekStart(metric.getWeekStart());
        entity.setProgressDelta(metric.getProgressDelta());
        entity.setActivityIndex(metric.getActivityIndex());
        entity.setActiveHours(metric.getActiveHours());
        entity.setRiskLevel(metric.getRiskLevel().toPersistenceValue());
        entity.setCreatedAt(metric.getCreatedAt());
        return toDomain(weeklyMetricRepository.save(entity));
    }

    @Override
    public List<WeeklyMetric> findLatest(Integer projectId, int limit) {
        return weeklyMetricRepository.findByProjectIdOrderByWeekStartDesc(projectId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public BigDecimal findAverageActivityBefore(Integer projectId, LocalDate weekStart) {
        return weeklyMetricRepository.findAverageActivityBefore(projectId, weekStart);
    }

    private WeeklyMetric toDomain(WeeklyMetricEntity entity) {
        return WeeklyMetric.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getWeekStart(),
                entity.getProgressDelta(),
                entity.getActivityIndex(),
                entity.getActiveHours(),
                RiskLevel.fromPersistenceValue(entity.getRiskLevel()),
                entity.getCreatedAt()
        );
    }
}

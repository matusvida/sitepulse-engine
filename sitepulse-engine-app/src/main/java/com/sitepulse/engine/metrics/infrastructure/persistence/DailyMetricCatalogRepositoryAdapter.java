package com.sitepulse.engine.metrics.infrastructure.persistence;

import com.sitepulse.engine.metrics.domain.DailyMetricEntity;
import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import com.sitepulse.engine.metrics.persistence.DailyMetricRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DailyMetricCatalogRepositoryAdapter implements DailyMetricCatalogRepository {

    private final DailyMetricRepository dailyMetricRepository;

    @Override
    public Optional<DailyMetric> findByProjectAndDate(Integer projectId, LocalDate date) {
        return dailyMetricRepository.findByProjectIdAndDate(projectId, date).map(this::toDomain);
    }

    @Override
    public DailyMetric save(DailyMetric metric) {
        DailyMetricEntity entity = metric.getId() == null
                ? new DailyMetricEntity()
                : dailyMetricRepository.findById(metric.getId()).orElseGet(DailyMetricEntity::new);
        entity.setProjectId(metric.getProjectId());
        entity.setDate(metric.getDate());
        entity.setPeopleCount(metric.getPeopleCount());
        entity.setVehicleCount(metric.getVehicleCount());
        entity.setActiveHours(metric.getActiveHours());
        entity.setCreatedAt(metric.getCreatedAt());
        return toDomain(dailyMetricRepository.save(entity));
    }

    @Override
    public List<DailyMetric> findSince(Integer projectId, LocalDate sinceDate) {
        return dailyMetricRepository.findByProjectIdAndDateGreaterThanEqualOrderByDateAsc(projectId, sinceDate).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DailyMetric> findBetween(Integer projectId, LocalDate from, LocalDate to) {
        return dailyMetricRepository.findByProjectIdAndDateBetweenOrderByDateAsc(projectId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DailyMetric> findAllSince(Integer projectId, LocalDate sinceDate) {
        return findSince(projectId, sinceDate);
    }

    private DailyMetric toDomain(DailyMetricEntity entity) {
        return DailyMetric.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getDate(),
                entity.getPeopleCount(),
                entity.getVehicleCount(),
                entity.getActiveHours(),
                entity.getCreatedAt()
        );
    }
}

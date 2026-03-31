package com.sitepulse.engine.alert.infrastructure.persistence;

import com.sitepulse.engine.alert.domain.AlertEntity;
import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.model.AlertSeverity;
import com.sitepulse.engine.alert.domain.model.AlertStatus;
import com.sitepulse.engine.alert.domain.port.AlertCatalogRepository;
import com.sitepulse.engine.alert.persistence.AlertRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlertCatalogRepositoryAdapter implements AlertCatalogRepository {

    private final AlertRepository alertRepository;

    @Override
    public List<Alert> findByProject(Integer projectId) {
        return alertRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Alert> findByIdAndProject(Integer alertId, Integer projectId) {
        return alertRepository.findByIdAndProjectId(alertId, projectId).map(this::toDomain);
    }

    @Override
    public boolean existsByProjectAndTypeAndStatus(Integer projectId, String type, AlertStatus status) {
        return alertRepository.existsByProjectIdAndTypeAndStatus(projectId, type, status.toPersistenceValue());
    }

    @Override
    public List<Alert> findByProjectAndTypeAndStatus(Integer projectId, String type, AlertStatus status) {
        return alertRepository.findByProjectIdAndTypeAndStatus(projectId, type, status.toPersistenceValue()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Alert save(Alert alert) {
        AlertEntity entity = alert.getId() == null
                ? new AlertEntity()
                : alertRepository.findById(alert.getId()).orElseGet(AlertEntity::new);
        entity.setProjectId(alert.getProjectId());
        entity.setType(alert.getType());
        entity.setSeverity(alert.getSeverity().toPersistenceValue());
        entity.setStatus(alert.getStatus().toPersistenceValue());
        entity.setSummary(alert.getSummary());
        entity.setDetails(alert.getDetails());
        entity.setRecommendedActions(alert.getRecommendedActions());
        entity.setCreatedAt(alert.getCreatedAt());
        entity.setUpdatedAt(alert.getUpdatedAt());
        return toDomain(alertRepository.save(entity));
    }

    @Override
    public List<Alert> saveAll(List<Alert> alerts) {
        return alerts.stream().map(this::save).toList();
    }

    private Alert toDomain(AlertEntity entity) {
        return Alert.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getType(),
                AlertSeverity.fromValue(entity.getSeverity()),
                AlertStatus.fromValue(entity.getStatus()),
                entity.getSummary(),
                entity.getDetails(),
                entity.getRecommendedActions(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

package com.sitepulse.engine.alert.infrastructure.persistence;

import com.sitepulse.engine.alert.domain.enums.AlertSeverity;
import com.sitepulse.engine.alert.domain.enums.AlertStatus;
import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.port.AlertReadModel;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertReadModelAdapter implements AlertReadModel {

    private final AlertRepository alertRepository;

    @Override
    public List<Alert> findByProjectFiltered(Integer projectId, String type, String severity, String status) {
        Stream<AlertEntity> stream = alertRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream();
        if (type != null && !type.isBlank()) {
            stream = stream.filter(e -> type.equalsIgnoreCase(e.getType()));
        }
        if (severity != null && !severity.isBlank()) {
            stream = stream.filter(e -> severity.equalsIgnoreCase(e.getSeverity()));
        }
        if (status != null && !status.isBlank()) {
            stream = stream.filter(e -> status.equalsIgnoreCase(e.getStatus()));
        }
        return stream.map(this::toDomain).toList();
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

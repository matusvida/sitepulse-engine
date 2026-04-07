package com.sitepulse.engine.alert.application.usecase;

import com.sitepulse.engine.alert.application.command.UpdateAlertStatusCommand;
import com.sitepulse.engine.alert.application.result.AlertResult;
import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.port.AlertCatalogRepository;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAlertStatusUseCase {

    private final AlertCatalogRepository alertCatalogRepository;

    @Transactional
    public AlertResult update(UpdateAlertStatusCommand command) {
        Alert alert = alertCatalogRepository.findByIdAndProject(command.alertId(), command.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        alert.updateStatus(command.status(), OffsetDateTime.now(ZoneOffset.UTC));
        return toResult(alertCatalogRepository.save(alert));
    }

    private AlertResult toResult(Alert alert) {
        return new AlertResult(
                alert.getId(),
                alert.getProjectId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getSummary(),
                alert.getDetails(),
                alert.getRecommendedActions(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }
}

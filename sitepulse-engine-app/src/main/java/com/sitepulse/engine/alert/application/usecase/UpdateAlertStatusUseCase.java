package com.sitepulse.engine.alert.application.usecase;

import com.sitepulse.engine.alert.application.result.AlertResult;
import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.model.AlertStatus;
import com.sitepulse.engine.alert.domain.port.AlertCatalogRepository;
import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.http.alert.dto.AlertStatusUpdateRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAlertStatusUseCase {

    private final AlertCatalogRepository alertCatalogRepository;

    @Transactional
    public AlertResult update(Integer projectId, Integer alertId, AlertStatusUpdateRequest request) {
        AlertStatus status = parseStatus(request.getStatus());
        Alert alert = alertCatalogRepository.findByIdAndProject(alertId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));
        alert.updateStatus(status, OffsetDateTime.now(ZoneOffset.UTC));
        return toResult(alertCatalogRepository.save(alert));
    }

    private AlertStatus parseStatus(String value) {
        try {
            return AlertStatus.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status. Must be open, acknowledged, or resolved");
        }
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

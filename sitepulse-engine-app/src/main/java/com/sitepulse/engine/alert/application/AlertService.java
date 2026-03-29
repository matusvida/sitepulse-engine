package com.sitepulse.engine.alert.application;

import com.sitepulse.engine.alert.domain.AlertEntity;
import com.sitepulse.engine.alert.persistence.AlertRepository;
import com.sitepulse.engine.http.alert.dto.AlertStatusUpdateRequest;
import com.sitepulse.engine.common.web.ApiException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public List<AlertEntity> listAlerts(Integer projectId, String type, String severity, String status) {
        if (type != null && severity != null && status != null) {
            return alertRepository.findByProjectIdAndTypeAndSeverityAndStatusOrderByCreatedAtDesc(projectId, type, severity, status);
        }
        return alertRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .filter(alert -> type == null || type.equals(alert.getType()))
                .filter(alert -> severity == null || severity.equals(alert.getSeverity()))
                .filter(alert -> status == null || status.equals(alert.getStatus()))
                .toList();
    }

    @Transactional
    public AlertEntity updateStatus(Integer projectId, Integer alertId, AlertStatusUpdateRequest request) {
        if (!List.of("open", "acknowledged", "resolved").contains(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status. Must be open, acknowledged, or resolved");
        }
        AlertEntity alert = alertRepository.findByIdAndProjectId(alertId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));
        alert.setStatus(request.getStatus());
        alert.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return alertRepository.save(alert);
    }

    public boolean hasOpenAlert(Integer projectId, String type) {
        return alertRepository.existsByProjectIdAndTypeAndStatus(projectId, type, "open");
    }

    @Transactional
    public void createAlert(Integer projectId, String type, String severity, String summary, String details, List<String> actions) {
        if (hasOpenAlert(projectId, type)) {
            return;
        }
        alertRepository.save(AlertEntity.builder()
                .projectId(projectId)
                .type(type)
                .severity(severity)
                .status("open")
                .summary(summary)
                .details(details)
                .recommendedActions(actions)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }

    @Transactional
    public int autoResolve(Integer projectId, String type) {
        List<AlertEntity> alerts = alertRepository.findByProjectIdAndTypeAndStatus(projectId, type, "open");
        alerts.forEach(alert -> {
            alert.setStatus("resolved");
            alert.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        });
        alertRepository.saveAll(alerts);
        return alerts.size();
    }
}

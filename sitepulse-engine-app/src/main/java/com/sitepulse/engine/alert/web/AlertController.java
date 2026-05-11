package com.sitepulse.engine.alert.web;

import com.sitepulse.engine.alert.application.command.UpdateAlertStatusCommand;
import com.sitepulse.engine.alert.application.result.AlertResult;
import com.sitepulse.engine.alert.application.usecase.ListProjectAlertsQuery;
import com.sitepulse.engine.alert.application.usecase.UpdateAlertStatusUseCase;
import com.sitepulse.engine.alert.domain.enums.AlertStatus;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.http.alert.api.AlertApi;
import com.sitepulse.engine.http.alert.dto.AlertStatusUpdateRequest;
import com.sitepulse.engine.http.alert.dto.AlertView;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlertController implements AlertApi {

    private final ProjectLookupService projectLookupService;
    private final ListProjectAlertsQuery listProjectAlertsQuery;
    private final UpdateAlertStatusUseCase updateAlertStatusUseCase;

    @Override
    @PreAuthorize("@projectAccessAuthorizationService.hasProjectAccess(authentication, #projectId)")
    public List<AlertView> listAlerts(Integer projectId, String type, String severity, String status) {
        projectLookupService.requireProject(projectId);
        return listProjectAlertsQuery.list(projectId, type, severity, status).stream()
                .map(this::toAlertView)
                .toList();
    }

    @Override
    @PreAuthorize("@projectAccessAuthorizationService.hasProjectAccess(authentication, #projectId)")
    public AlertView updateAlert(Integer projectId, Integer alertId, AlertStatusUpdateRequest request) {
        projectLookupService.requireProject(projectId);
        AlertStatus status = parseAlertStatus(request.getStatus());
        return toAlertView(updateAlertStatusUseCase.update(new UpdateAlertStatusCommand(projectId, alertId, status)));
    }

    private AlertView toAlertView(AlertResult alert) {
        return new AlertView(
                alert.getId(),
                alert.getProjectId(),
                alert.getType(),
                alert.getSeverity().toPersistenceValue(),
                alert.getStatus().toPersistenceValue(),
                alert.getSummary(),
                alert.getDetails(),
                alert.getRecommendedActions(),
                alert.getCreatedAt() == null ? null : alert.getCreatedAt().toString(),
                alert.getUpdatedAt() == null ? null : alert.getUpdatedAt().toString()
        );
    }

    private AlertStatus parseAlertStatus(String value) {
        try {
            return AlertStatus.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid status. Must be open, acknowledged, or resolved");
        }
    }
}

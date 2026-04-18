package com.sitepulse.engine.alert.application.usecase;

import com.sitepulse.engine.alert.application.result.AlertResult;
import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.port.AlertReadModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectAlertsQuery {

    private final AlertReadModel alertReadModel;

    public List<AlertResult> list(Integer projectId, String type, String severity, String status) {
        return alertReadModel.findByProjectFiltered(projectId, type, severity, status).stream()
                .map(this::toResult)
                .toList();
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

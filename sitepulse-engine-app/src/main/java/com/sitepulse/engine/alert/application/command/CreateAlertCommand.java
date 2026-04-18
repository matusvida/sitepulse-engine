package com.sitepulse.engine.alert.application.command;

import com.sitepulse.engine.alert.domain.enums.AlertSeverity;
import java.util.List;

public record CreateAlertCommand(
        Integer projectId,
        String type,
        AlertSeverity severity,
        String summary,
        String details,
        List<String> recommendedActions
) {
}

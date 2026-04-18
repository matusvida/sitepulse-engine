package com.sitepulse.engine.alert.application.command;

import com.sitepulse.engine.alert.domain.enums.AlertStatus;

public record UpdateAlertStatusCommand(Integer projectId, Integer alertId, AlertStatus status) {
}

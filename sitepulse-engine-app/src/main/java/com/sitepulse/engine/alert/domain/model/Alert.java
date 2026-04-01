package com.sitepulse.engine.alert.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Alert {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final Integer id;

    private final Integer projectId;
    private final String type;
    private final AlertSeverity severity;

    @ToString.Include
    private AlertStatus status;
    private final String summary;
    private final String details;
    private final List<String> recommendedActions;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static Alert create(
            Integer projectId,
            String type,
            AlertSeverity severity,
            String summary,
            String details,
            List<String> recommendedActions,
            OffsetDateTime createdAt
    ) {
        return new Alert(null, projectId, type, severity, AlertStatus.OPEN, summary, details, recommendedActions, createdAt, null);
    }

    public static Alert restore(
            Integer id,
            Integer projectId,
            String type,
            AlertSeverity severity,
            AlertStatus status,
            String summary,
            String details,
            List<String> recommendedActions,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new Alert(id, projectId, type, severity, status, summary, details, recommendedActions, createdAt, updatedAt);
    }

    public void acknowledge(OffsetDateTime updatedAt) {
        if (status != AlertStatus.OPEN) {
            throw new IllegalStateException("Can only acknowledge OPEN alerts, current: " + status);
        }
        status = AlertStatus.ACKNOWLEDGED;
        this.updatedAt = updatedAt;
    }

    public void resolve(OffsetDateTime updatedAt) {
        if (status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Alert is already resolved");
        }
        status = AlertStatus.RESOLVED;
        this.updatedAt = updatedAt;
    }

    public void updateStatus(AlertStatus newStatus, OffsetDateTime updatedAt) {
        switch (newStatus) {
            case ACKNOWLEDGED -> acknowledge(updatedAt);
            case RESOLVED -> resolve(updatedAt);
            default -> throw new IllegalArgumentException("Cannot transition to status: " + newStatus);
        }
    }
}

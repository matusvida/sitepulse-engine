package com.sitepulse.engine.alert.application.result;

import com.sitepulse.engine.alert.domain.enums.AlertSeverity;
import com.sitepulse.engine.alert.domain.enums.AlertStatus;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AlertResult {

    private Integer id;
    private Integer projectId;
    private String type;
    private AlertSeverity severity;
    private AlertStatus status;
    private String summary;
    private String details;
    private List<String> recommendedActions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

package com.sitepulse.engine.alert.domain.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AlertRaisedEvent implements DomainEvent {

    private final Integer alertId;
    private final Integer projectId;
    private final String type;
    private final String severity;
}

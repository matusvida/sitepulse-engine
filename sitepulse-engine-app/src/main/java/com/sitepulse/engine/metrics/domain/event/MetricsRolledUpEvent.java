package com.sitepulse.engine.metrics.domain.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MetricsRolledUpEvent implements DomainEvent {

    private final Integer projectId;
    private final int daysProcessed;
    private final int weeksProcessed;
}

package com.sitepulse.engine.plan.domain.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MilestoneDelayedEvent implements DomainEvent {

    private final Integer projectId;
    private final Integer milestoneId;
    private final Integer weekNumber;
    private final String title;
    private final String actualState;
}

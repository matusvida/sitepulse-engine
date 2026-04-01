package com.sitepulse.engine.sync.domain.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ProjectSyncStartedEvent implements DomainEvent {

    private final Integer projectId;
    private final Integer syncJobId;
    private final OffsetDateTime startedAt;
}

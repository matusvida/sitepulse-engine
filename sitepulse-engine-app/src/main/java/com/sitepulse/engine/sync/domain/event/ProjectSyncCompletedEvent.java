package com.sitepulse.engine.sync.domain.event;

import com.sitepulse.engine.common.domain.event.DomainEvent;
import com.sitepulse.engine.sync.domain.model.SyncJobStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ProjectSyncCompletedEvent implements DomainEvent {

    private final Integer projectId;
    private final Integer syncJobId;
    private final SyncJobStatus status;
    private final int imagesFound;
    private final int imagesSynced;
}

package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.sync.application.result.SyncStatusResult;
import com.sitepulse.engine.sync.domain.port.SyncStatusReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectSyncStatusQuery {

    private final ProjectLookupService projectLookupService;
    private final SyncStatusReadModel syncStatusReadModel;

    public SyncStatusResult getLatest(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return syncStatusReadModel.getLatestStatus(projectId);
    }
}

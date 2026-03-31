package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.sync.application.result.SyncStatusResult;
import com.sitepulse.engine.sync.domain.model.SyncJob;
import com.sitepulse.engine.sync.domain.port.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectSyncStatusQuery {

    private final ProjectLookupService projectLookupService;
    private final SyncJobRepository syncJobRepository;

    public SyncStatusResult getLatest(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return syncJobRepository.findLatestForProject(projectId)
                .map(this::toResult)
                .orElseGet(() -> SyncStatusResult.neverRun(projectId));
    }

    private SyncStatusResult toResult(SyncJob syncJob) {
        return new SyncStatusResult(
                syncJob.getId(),
                syncJob.getProjectId(),
                syncJob.getStatus(),
                null,
                syncJob.getImagesFound(),
                syncJob.getImagesSynced(),
                syncJob.errorSummary(),
                syncJob.getStartedAt(),
                syncJob.getFinishedAt(),
                false
        );
    }
}

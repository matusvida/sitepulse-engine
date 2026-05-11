package com.sitepulse.engine.sync.web;

import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.project.dto.SyncStatusView;
import com.sitepulse.engine.http.sync.api.SyncApi;
import com.sitepulse.engine.sync.application.result.SyncStatusResult;
import com.sitepulse.engine.sync.application.usecase.GetProjectSyncStatusQuery;
import com.sitepulse.engine.sync.application.usecase.TriggerProjectSyncUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SyncController implements SyncApi {

    private final GetProjectSyncStatusQuery getProjectSyncStatusQuery;
    private final TriggerProjectSyncUseCase triggerProjectSyncUseCase;

    @Override
    @PreAuthorize("@projectAccessAuthorizationService.hasProjectAccess(authentication, #projectId)")
    public SyncStatusView syncStatus(Integer projectId) {
        SyncStatusResult result = getProjectSyncStatusQuery.getLatest(projectId);
        if (result.isNeverRun()) {
            return new SyncStatusView(
                    null,
                    String.valueOf(result.getProjectId()),
                    "never_run",
                    result.getMessage(),
                    null, null, null, null, null
            );
        }
        return new SyncStatusView(
                String.valueOf(result.getJobId()),
                String.valueOf(result.getProjectId()),
                result.getStatus().name(),
                result.getMessage(),
                result.getImagesFound(),
                result.getImagesSynced(),
                result.getError(),
                result.getStartedAt() == null ? null : result.getStartedAt().toString(),
                result.getFinishedAt() == null ? null : result.getFinishedAt().toString()
        );
    }

    @Override
    @PreAuthorize("@projectAccessAuthorizationService.hasProjectAccess(authentication, #projectId)")
    public ActionResponse triggerSync(Integer projectId) {
        triggerProjectSyncUseCase.trigger(projectId);
        return new ActionResponse("accepted", "Sync job started in background", projectId);
    }
}

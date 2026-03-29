package com.sitepulse.engine.sync.application;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.project.application.ProjectService;
import com.sitepulse.engine.project.domain.ProjectEntity;
import com.sitepulse.engine.sync.domain.SyncJobEntity;
import com.sitepulse.engine.sync.persistence.SyncJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncService {

    private final ProjectService projectService;
    private final SyncJobRepository syncJobRepository;
    private final SyncProjectExecutor syncProjectExecutor;

    public SyncJobEntity latestSyncJob(Integer projectId) {
        return syncJobRepository.findTopByProjectIdOrderByStartedAtDesc(projectId).orElse(null);
    }

    @Async("applicationTaskExecutor")
    public void triggerProjectSync(Integer projectId) {
        log.info("Starting manual sync trigger for projectId={}", projectId);
        syncProjectExecutor.syncProject(requireSyncableProject(projectId));
    }

    @Async("applicationTaskExecutor")
    public void syncAllProjects(List<ProjectEntity> projects) {
        log.info("Starting scheduled sync for {} projects", projects.size());
        projects.stream()
                .filter(project -> project.getDropboxPath() != null && !project.getDropboxPath().isBlank())
                .forEach(syncProjectExecutor::syncProject);
    }

    public ProjectEntity requireSyncableProject(Integer projectId) {
        ProjectEntity project = projectService.requireProject(projectId);
        if (project.getDropboxPath() == null || project.getDropboxPath().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Project has no dropboxPath configured");
        }
        return project;
    }

}

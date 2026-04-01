package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TriggerProjectSyncUseCase {

    private final ProjectLookupService projectLookupService;
    private final RunProjectSyncUseCase runProjectSyncUseCase;

    @Async("applicationTaskExecutor")
    public void trigger(Integer projectId) {
        Project project = requireSyncableProject(projectId);
        log.info("Starting manual sync trigger for projectId={}", projectId);
        runProjectSyncUseCase.run(project);
    }

    private Project requireSyncableProject(Integer projectId) {
        Project project = projectLookupService.requireProject(projectId);
        if (project.getDropboxPath() == null || project.getDropboxPath().isBlank()) {
            throw new ValidationException("Project has no dropboxPath configured");
        }
        return project;
    }
}

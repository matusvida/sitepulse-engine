package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunScheduledSyncUseCase {

    private final ProjectCatalogRepository projectCatalogRepository;
    private final CameraCatalogRepository cameraCatalogRepository;
    private final RunProjectSyncUseCase runProjectSyncUseCase;

    @Async("applicationTaskExecutor")
    public void run() {
        var projects = projectCatalogRepository.findAll();
        log.info("Starting scheduled sync for {} projects", projects.size());
        projects.stream()
                .filter(this::isSyncable)
                .forEach(runProjectSyncUseCase::run);
    }

    private boolean isSyncable(com.sitepulse.engine.project.domain.model.Project project) {
        return cameraCatalogRepository.findByProjectId(project.getId()).stream()
                .anyMatch(camera -> camera.getDropboxPath() != null && !camera.getDropboxPath().isBlank());
    }
}

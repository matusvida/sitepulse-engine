package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunScheduledSyncUseCase {

    private final ProjectCatalogRepository projectCatalogRepository;
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
        return project.getDropboxPath() != null && !project.getDropboxPath().isBlank();
    }
}

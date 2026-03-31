package com.sitepulse.engine.metrics.application.usecase;

import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunScheduledAnalysisUseCase {

    private final ProjectCatalogRepository projectCatalogRepository;
    private final RunProjectAnalysisUseCase runProjectAnalysisUseCase;

    public void run() {
        projectCatalogRepository.findAll().forEach(project -> {
            log.info("Running scheduled analysis for projectId={}", project.getId());
            runProjectAnalysisUseCase.run(project.getId(), 7);
        });
    }
}

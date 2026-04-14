package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.ProjectResultMapper;
import com.sitepulse.engine.project.application.command.UpdateProjectCommand;
import com.sitepulse.engine.project.application.result.ProjectResult;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import com.sitepulse.engine.common.exception.ValidationException;
import java.time.DateTimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final ProjectLookupService projectLookupService;
    private final ProjectCatalogRepository projectCatalogRepository;
    private final ProjectResultMapper projectResultMapper;

    @Transactional
    public ProjectResult update(UpdateProjectCommand command) {
        try {
            Project project = projectLookupService.requireProject(command.projectId());
            project.update(command.name(), command.location(), command.storageKeyPrefix(), command.timezone());
            project = projectCatalogRepository.save(project);
            return projectResultMapper.toResult(project);
        } catch (DateTimeException | IllegalArgumentException ex) {
            throw new ValidationException("Invalid timezone");
        }
    }
}

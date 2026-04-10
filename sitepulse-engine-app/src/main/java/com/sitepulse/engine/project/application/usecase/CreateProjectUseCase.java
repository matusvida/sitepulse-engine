package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.ProjectResultMapper;
import com.sitepulse.engine.project.application.command.CreateProjectCommand;
import com.sitepulse.engine.project.application.result.ProjectResult;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectCatalogRepository projectCatalogRepository;
    private final ProjectResultMapper projectResultMapper;

    @Transactional
    public ProjectResult create(CreateProjectCommand command) {
        Project project = projectCatalogRepository.save(
                Project.create(command.name(), command.location(), command.storageKeyPrefix(), OffsetDateTime.now(ZoneOffset.UTC))
        );
        return projectResultMapper.toResult(project);
    }
}

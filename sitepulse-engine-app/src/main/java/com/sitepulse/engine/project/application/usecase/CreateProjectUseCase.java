package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.project.application.command.CreateProjectCommand;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import com.sitepulse.engine.project.domain.port.ProjectReadModel;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectCatalogRepository projectCatalogRepository;
    private final ProjectReadModel projectReadModel;

    @Transactional
    public ProjectView create(CreateProjectCommand command) {
        Project project = projectCatalogRepository.save(
                Project.create(command.name(), command.location(), command.dropboxPath(), OffsetDateTime.now(ZoneOffset.UTC))
        );
        return new ProjectView(
                String.valueOf(project.getId()),
                project.getName(),
                project.getLocation() == null ? "" : project.getLocation(),
                0,
                projectReadModel.countCameras(project.getId()),
                "",
                project.getDropboxPath(),
                project.getCreatedAt() == null ? null : project.getCreatedAt().toString()
        );
    }
}

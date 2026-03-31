package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.command.UpdateProjectCommand;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import com.sitepulse.engine.project.domain.port.ProjectReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final ProjectLookupService projectLookupService;
    private final ProjectCatalogRepository projectCatalogRepository;
    private final ProjectReadModel projectReadModel;

    @Transactional
    public ProjectView update(UpdateProjectCommand command) {
        Project project = projectLookupService.requireProject(command.projectId());
        project.update(command.name(), command.location(), command.dropboxPath());
        project = projectCatalogRepository.save(project);
        return new ProjectView(
                String.valueOf(project.getId()),
                project.getName(),
                project.getLocation() == null ? "" : project.getLocation(),
                0,
                projectReadModel.countCameras(project.getId()),
                projectReadModel.latestSnapshotAt(project.getId()).map(OffsetDateTime -> OffsetDateTime.toString()).orElse(""),
                project.getDropboxPath(),
                project.getCreatedAt() == null ? null : project.getCreatedAt().toString()
        );
    }
}

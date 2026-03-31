package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectQuery {

    private final ProjectLookupService projectLookupService;
    private final ProjectReadModel projectReadModel;

    public ProjectView get(Integer projectId) {
        return toView(projectLookupService.requireProject(projectId));
    }

    private ProjectView toView(Project project) {
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

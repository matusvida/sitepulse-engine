package com.sitepulse.engine.project.application;

import com.sitepulse.engine.project.application.result.CameraResult;
import com.sitepulse.engine.project.application.result.ProjectResult;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectResultMapper {

    private final ProjectReadModel projectReadModel;

    public ProjectResult toResult(Project project) {
        return new ProjectResult(
                project.getId(),
                project.getName(),
                project.getLocation() == null ? "" : project.getLocation(),
                0,
                projectReadModel.countCameras(project.getId()),
                projectReadModel.latestSnapshotAt(project.getId()).map(odt -> odt.toString()).orElse(""),
                project.getStorageKeyPrefix(),
                project.getCreatedAt()
        );
    }

    public CameraResult toResult(Camera camera) {
        return new CameraResult(
                camera.getId(),
                camera.getProjectId(),
                camera.getName(),
                camera.getDropboxPath(),
                camera.getRoiPolygon(),
                camera.getDropOutside(),
                camera.getKeyPrefix(),
                camera.getCreatedAt()
        );
    }
}

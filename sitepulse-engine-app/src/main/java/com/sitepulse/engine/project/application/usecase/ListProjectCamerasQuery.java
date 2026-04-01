package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectCamerasQuery {

    private final ProjectLookupService projectLookupService;
    private final CameraCatalogRepository cameraCatalogRepository;

    public List<CameraView> get(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return cameraCatalogRepository.findByProjectId(projectId).stream().map(this::toView).toList();
    }

    private CameraView toView(Camera camera) {
        return new CameraView(
                camera.getId(),
                camera.getProjectId(),
                camera.getName(),
                camera.getRoiPolygon(),
                camera.getDropOutside(),
                camera.getKeyPrefix(),
                camera.getCreatedAt() == null ? null : camera.getCreatedAt().toString()
        );
    }
}

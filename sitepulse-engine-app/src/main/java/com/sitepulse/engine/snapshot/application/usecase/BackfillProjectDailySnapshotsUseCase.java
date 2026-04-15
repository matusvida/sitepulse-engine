package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BackfillProjectDailySnapshotsUseCase {

    private final ProjectLookupService projectLookupService;
    private final CameraCatalogRepository cameraCatalogRepository;
    private final BackfillCameraDailySnapshotsUseCase backfillCameraDailySnapshotsUseCase;

    public void backfill(Integer projectId, Integer cameraId, boolean force) {
        Project project = projectLookupService.requireProject(projectId);
        List<Camera> cameras = resolveCameras(projectId, cameraId);
        cameras.forEach(camera -> backfillCameraDailySnapshotsUseCase.backfill(project, camera, force));
    }

    private List<Camera> resolveCameras(Integer projectId, Integer cameraId) {
        if (cameraId == null) {
            return cameraCatalogRepository.findByProjectId(projectId);
        }
        return List.of(cameraCatalogRepository.findByIdAndProjectId(cameraId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found")));
    }
}

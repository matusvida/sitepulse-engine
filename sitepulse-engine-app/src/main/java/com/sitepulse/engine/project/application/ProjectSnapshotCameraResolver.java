package com.sitepulse.engine.project.application;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectSnapshotCameraResolver {

    private final CameraCatalogRepository cameraCatalogRepository;

    public Camera resolve(Integer projectId) {
        // The project-scoped snapshot endpoint currently represents the first configured camera.
        // This preserves stable behavior until a camera-scoped public endpoint is introduced.
        return cameraCatalogRepository.findByProjectId(projectId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Project has no cameras"));
    }
}

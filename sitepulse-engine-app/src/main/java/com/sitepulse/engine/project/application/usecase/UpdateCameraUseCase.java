package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.ProjectResultMapper;
import com.sitepulse.engine.project.application.command.UpdateCameraCommand;
import com.sitepulse.engine.project.application.result.CameraResult;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCameraUseCase {

    private final ProjectLookupService projectLookupService;
    private final CameraCatalogRepository cameraCatalogRepository;
    private final ProjectResultMapper projectResultMapper;

    @Transactional
    public CameraResult update(UpdateCameraCommand command) {
        projectLookupService.requireProject(command.projectId());
        Camera camera = cameraCatalogRepository.findByIdAndProjectId(command.cameraId(), command.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Camera not found"));
        camera.update(command.roiPolygon(), command.dropOutside());
        camera = cameraCatalogRepository.save(camera);
        return projectResultMapper.toResult(camera);
    }
}

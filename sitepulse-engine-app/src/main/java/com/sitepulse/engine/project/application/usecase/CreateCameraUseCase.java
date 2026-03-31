package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.command.CreateCameraCommand;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCameraUseCase {

    private final ProjectLookupService projectLookupService;
    private final CameraCatalogRepository cameraCatalogRepository;

    @Transactional
    public CameraView create(CreateCameraCommand command) {
        projectLookupService.requireProject(command.projectId());
        Camera camera = cameraCatalogRepository.save(
                Camera.create(
                        command.projectId(),
                        command.name(),
                        command.keyPrefix(),
                        command.roiPolygon(),
                        command.dropOutside(),
                        OffsetDateTime.now(ZoneOffset.UTC)
                )
        );
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

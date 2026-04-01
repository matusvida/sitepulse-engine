package com.sitepulse.engine.detection.infrastructure.external;

import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CameraLookupAdapter implements CameraLookup {

    private final CameraCatalogRepository cameraCatalogRepository;

    @Override
    public Integer findCameraIdByProjectAndKey(Integer projectId, String key) {
        return cameraCatalogRepository.findByProjectId(projectId).stream()
                .filter(camera -> camera.getKeyPrefix() != null && key.startsWith(camera.getKeyPrefix()))
                .findFirst()
                .map(Camera::getId)
                .orElse(null);
    }

    @Override
    public CameraRoiSettings findRoiSettings(Integer projectId, String key) {
        return cameraCatalogRepository.findByProjectId(projectId).stream()
                .filter(camera -> camera.getKeyPrefix() != null && key.startsWith(camera.getKeyPrefix()))
                .findFirst()
                .map(camera -> new CameraRoiSettings(camera.getRoiPolygon(), Boolean.TRUE.equals(camera.getDropOutside())))
                .orElse(null);
    }
}

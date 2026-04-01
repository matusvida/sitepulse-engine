package com.sitepulse.engine.detection.infrastructure.external;

import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.project.infrastructure.persistence.CameraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CameraLookupAdapter implements CameraLookup {

    private final CameraRepository cameraRepository;

    @Override
    public Integer findCameraIdByProjectAndKey(Integer projectId, String key) {
        return cameraRepository.findByProjectIdAndKeyPrefixIsNotNullOrderByKeyPrefixDesc(projectId).stream()
                .filter(camera -> key.startsWith(camera.getKeyPrefix()))
                .findFirst()
                .map(camera -> camera.getId())
                .orElse(null);
    }

    @Override
    public CameraRoiSettings findRoiSettings(Integer projectId, String key) {
        return cameraRepository.findByProjectIdAndKeyPrefixIsNotNullOrderByKeyPrefixDesc(projectId).stream()
                .filter(camera -> key.startsWith(camera.getKeyPrefix()))
                .findFirst()
                .map(camera -> new CameraRoiSettings(camera.getRoiPolygon(), Boolean.TRUE.equals(camera.getDropOutside())))
                .orElse(null);
    }
}

package com.sitepulse.engine.detection.infrastructure.external;

import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CameraLookupAdapter implements CameraLookup {

    private final CameraCatalogRepository cameraCatalogRepository;
    private final ProjectCatalogRepository projectCatalogRepository;

    @Override
    public Integer findCameraIdByProjectAndKey(Integer projectId, String key) {
        return findCamera(projectId, key)
                .map(Camera::getId)
                .orElse(null);
    }

    @Override
    public CameraRoiSettings findRoiSettings(Integer projectId, String key) {
        return findCamera(projectId, key)
                .map(camera -> new CameraRoiSettings(camera.getRoiPolygon(), Boolean.TRUE.equals(camera.getDropOutside())))
                .orElse(null);
    }

    @Override
    public Integer findImageWidth(Integer projectId, String key) {
        return findCamera(projectId, key).map(Camera::getImageWidth).orElse(null);
    }

    @Override
    public Integer findImageHeight(Integer projectId, String key) {
        return findCamera(projectId, key).map(Camera::getImageHeight).orElse(null);
    }

    private java.util.Optional<Camera> findCamera(Integer projectId, String key) {
        String projectPrefix = projectCatalogRepository.findById(projectId)
                .map(project -> normalizePrefix(project.getStorageKeyPrefix()))
                .orElse(null);
        return cameraCatalogRepository.findByProjectId(projectId).stream()
                .filter(camera -> matchesKey(projectPrefix, camera, key))
                .max(Comparator.comparingInt(camera -> expectedPrefix(projectPrefix, camera).length()));
    }

    private boolean matchesKey(String projectPrefix, Camera camera, String key) {
        String expected = expectedPrefix(projectPrefix, camera);
        return !expected.isBlank() && (key.equals(expected) || key.startsWith(expected + "/"));
    }

    private String expectedPrefix(String projectPrefix, Camera camera) {
        String cameraPrefix = normalizePrefix(camera.getKeyPrefix());
        if (cameraPrefix == null) {
            return "";
        }
        if (projectPrefix == null) {
            return cameraPrefix;
        }
        return projectPrefix + "/" + cameraPrefix;
    }

    private String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}

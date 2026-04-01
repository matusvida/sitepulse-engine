package com.sitepulse.engine.project.domain.port;

import com.sitepulse.engine.project.domain.model.Camera;
import java.util.List;
import java.util.Optional;

public interface CameraCatalogRepository {

    List<Camera> findByProjectId(Integer projectId);

    Optional<Camera> findByIdAndProjectId(Integer cameraId, Integer projectId);

    Camera save(Camera camera);
}

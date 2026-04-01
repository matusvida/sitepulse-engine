package com.sitepulse.engine.project.infrastructure.persistence;

import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CameraCatalogRepositoryAdapter implements CameraCatalogRepository {

    private final CameraRepository cameraRepository;

    @Override
    public List<Camera> findByProjectId(Integer projectId) {
        return cameraRepository.findByProjectIdOrderById(projectId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Camera> findByIdAndProjectId(Integer cameraId, Integer projectId) {
        return cameraRepository.findByIdAndProjectId(cameraId, projectId).map(this::toDomain);
    }

    @Override
    public Camera save(Camera camera) {
        CameraEntity entity = CameraEntity.builder()
                .id(camera.getId())
                .projectId(camera.getProjectId())
                .name(camera.getName())
                .roiPolygon(camera.getRoiPolygon())
                .dropOutside(camera.getDropOutside())
                .keyPrefix(camera.getKeyPrefix())
                .createdAt(camera.getCreatedAt())
                .build();
        return toDomain(cameraRepository.save(entity));
    }

    private Camera toDomain(CameraEntity entity) {
        return Camera.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getRoiPolygon(),
                entity.getDropOutside(),
                entity.getKeyPrefix(),
                entity.getCreatedAt()
        );
    }
}

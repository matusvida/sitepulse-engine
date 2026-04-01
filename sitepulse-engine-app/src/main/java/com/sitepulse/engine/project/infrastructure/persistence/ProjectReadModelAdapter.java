package com.sitepulse.engine.project.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.domain.port.ProjectReadModel;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectReadModelAdapter implements ProjectReadModel {

    private final CameraRepository cameraRepository;
    private final ProcessedImageReadModel processedImageReadModel;

    @Override
    public int countCameras(Integer projectId) {
        return cameraRepository.findByProjectIdOrderById(projectId).size();
    }

    @Override
    public Optional<OffsetDateTime> latestSnapshotAt(Integer projectId) {
        return processedImageReadModel.findProcessedByProject(projectId).stream()
                .map(image -> image.getCapturedAt())
                .filter(capturedAt -> capturedAt != null)
                .findFirst();
    }
}

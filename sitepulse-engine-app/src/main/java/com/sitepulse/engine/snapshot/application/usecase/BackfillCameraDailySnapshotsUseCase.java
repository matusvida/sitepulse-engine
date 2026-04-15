package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BackfillCameraDailySnapshotsUseCase {

    private final ImageRepository imageRepository;
    private final SnapshotTimezoneResolver timezoneResolver;
    private final GenerateCameraDailySnapshotUseCase generateUseCase;

    public void backfill(Project project, Camera camera, boolean force) {
        imageRepository.findAvailableSnapshotDatesByCameraId(camera.getId(), timezoneResolver.resolve(project).getId()).stream()
                .map(java.sql.Date::toLocalDate)
                .forEach(date -> generateUseCase.generate(project, camera, date, force));
    }
}

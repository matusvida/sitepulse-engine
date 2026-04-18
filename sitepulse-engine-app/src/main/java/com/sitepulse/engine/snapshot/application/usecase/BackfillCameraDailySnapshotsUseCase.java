package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import com.sitepulse.engine.snapshot.domain.port.SnapshotSourceImageReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BackfillCameraDailySnapshotsUseCase {

    private final SnapshotSourceImageReadModel snapshotSourceImageReadModel;
    private final SnapshotTimezoneResolver timezoneResolver;
    private final GenerateCameraDailySnapshotUseCase generateUseCase;

    public void backfill(Project project, Camera camera, boolean force) {
        snapshotSourceImageReadModel.findAvailableSnapshotDatesByCameraId(camera.getId(), timezoneResolver.resolve(project).getId()).stream()
                .forEach(date -> generateUseCase.generate(project, camera, date, force));
    }
}

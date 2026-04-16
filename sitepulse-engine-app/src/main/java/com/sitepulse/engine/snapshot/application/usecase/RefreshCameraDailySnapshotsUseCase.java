package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshCameraDailySnapshotsUseCase {

    private final GenerateCameraDailySnapshotUseCase generateUseCase;
    private final SnapshotTimezoneResolver timezoneResolver;
    private final Clock clock;

    public void refresh(Project project, Camera camera) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(timezoneResolver.resolve(project));
        refresh(project, camera, now.toLocalDate());
    }

    public void refresh(Project project, Camera camera, DetectionImage importedImage, byte[] importedSourceBytes) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(timezoneResolver.resolve(project));
        refresh(project, camera, now.toLocalDate(), importedImage, importedSourceBytes);
    }

    public void refresh(Project project, Camera camera, LocalDate snapshotDate) {
        refresh(project, camera, snapshotDate, null, null);
    }

    public void refresh(Project project, Camera camera, LocalDate snapshotDate, DetectionImage importedImage, byte[] importedSourceBytes) {
        try {
            generateUseCase.generate(project, camera, snapshotDate, false, importedImage, importedSourceBytes);
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh camera daily snapshot projectId={} cameraId={} date={} reason={}",
                    project.getId(), camera.getId(), snapshotDate, ex.getMessage());
        }
    }
}

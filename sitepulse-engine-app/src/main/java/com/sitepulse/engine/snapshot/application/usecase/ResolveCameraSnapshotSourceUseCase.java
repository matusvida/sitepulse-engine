package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotSourceDecision;
import com.sitepulse.engine.snapshot.application.service.CameraSnapshotProfileService;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResolveCameraSnapshotSourceUseCase {

    private static final int SOURCE_CANDIDATE_LIMIT = 20;
    private final ImageRepository imageRepository;
    private final CameraSnapshotProfileService profileService;
    private final SnapshotTimezoneResolver timezoneResolver;
    private final Clock clock;

    public CameraSnapshotSourceDecision resolve(Project project, Camera camera, LocalDate snapshotDate) {
        var zone = timezoneResolver.resolve(project);
        CameraSnapshotProfile profile = profileService.getOrCreate(camera.getId());
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        LocalDate today = now.toLocalDate();
        if (snapshotDate.isAfter(today)) {
            throw new ResourceNotFoundException("No image found for " + snapshotDate);
        }

        ZonedDateTime dayStart = snapshotDate.atStartOfDay(zone);
        ZonedDateTime dayEnd = snapshotDate.plusDays(1).atStartOfDay(zone);
        ZonedDateTime midday = snapshotDate.atTime(12, 0).atZone(zone);

        boolean frozen = snapshotDate.isBefore(today) || !now.toLocalTime().isBefore(profile.freezeTime());
        List<ImageEntity> sourceImages = frozen
                ? imageRepository.findRepresentativeSnapshotCandidatesByCameraId(
                        camera.getId(), toUtc(dayStart), toUtc(dayEnd), toUtc(midday), SOURCE_CANDIDATE_LIMIT)
                : imageRepository.findLatestSnapshotCandidatesByCameraId(
                        camera.getId(), toUtc(dayStart), toUtc(dayEnd), SOURCE_CANDIDATE_LIMIT);
        if (sourceImages.isEmpty()) {
            throw new ResourceNotFoundException("No image found for " + snapshotDate);
        }
        return new CameraSnapshotSourceDecision(sourceImages, frozen);
    }

    private OffsetDateTime toUtc(ZonedDateTime value) {
        return value.toOffsetDateTime();
    }
}

package com.sitepulse.engine.project.application;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.application.result.ProjectSnapshotMetadataResult;
import com.sitepulse.engine.project.application.result.ProjectSnapshotSelectionResult;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotAsset;
import com.sitepulse.engine.snapshot.application.usecase.GenerateCameraDailySnapshotUseCase;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import com.sitepulse.engine.snapshot.domain.port.SnapshotSourceImageReadModel;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectSnapshotService {

    private final ProjectLookupService projectLookupService;
    private final ProjectSnapshotCameraResolver cameraResolver;
    private final ProcessedImageReadModel processedImageReadModel;
    private final GenerateCameraDailySnapshotUseCase generateCameraDailySnapshotUseCase;
    private final SnapshotSourceImageReadModel snapshotSourceImageReadModel;
    private final SnapshotTimezoneResolver timezoneResolver;
    private final ObjectStorage objectStorage;
    private final SitePulseProperties properties;
    private final Clock clock;

    public List<LocalDate> listAvailableDates(Integer projectId) {
        if (!properties.imageWebSnapshots().enabled()) {
            projectLookupService.requireProject(projectId);
            return processedImageReadModel.findSnapshotCapturedAtValues(projectId).stream()
                    .map(instant -> instant.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate())
                    .distinct()
                    .toList();
        }
        Project project = projectLookupService.requireProject(projectId);
        Camera camera = cameraResolver.resolve(projectId);
        return snapshotSourceImageReadModel.findAvailableSnapshotDatesByCameraId(camera.getId(), timezoneResolver.resolve(project).getId());
    }

    public List<ProjectSnapshotMetadataResult> list(Integer projectId) {
        var ttl = properties.storagePresignTtl();
        OffsetDateTime expiresAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).plus(ttl);
        if (!properties.imageWebSnapshots().enabled()) {
            return processedImageReadModel.findRepresentativeSnapshots(projectId).stream()
                    .map(image -> new ProjectSnapshotMetadataResult(
                            image.getCapturedAt().toLocalDate(),
                            objectStorage.presign(image.getBucket(), image.getKey(), ttl),
                            expiresAt,
                            detectMediaType(image.getKey())))
                    .toList();
        }
        Project project = projectLookupService.requireProject(projectId);
        Camera camera = cameraResolver.resolve(projectId);
        return listAvailableDates(projectId).stream()
                .map(date -> toMetadata(generate(project, camera, date), ttl, expiresAt))
                .toList();
    }

    public ProjectSnapshotSelectionResult resolve(Integer projectId, LocalDate date) {
        if (!properties.imageWebSnapshots().enabled()) {
            projectLookupService.requireProject(projectId);
            var dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
            var dayEnd = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            var midday = date.atTime(12, 0).atOffset(ZoneOffset.UTC);
            StoredImage image = processedImageReadModel.findClosestSnapshot(projectId, dayStart, dayEnd, midday)
                    .orElseThrow(() -> new ResourceNotFoundException("No image found for " + date));
            return new ProjectSnapshotSelectionResult(date, image.getBucket(), image.getKey(), detectMediaType(image.getKey()));
        }
        Project project = projectLookupService.requireProject(projectId);
        Camera camera = cameraResolver.resolve(projectId);
        CameraSnapshotAsset snapshotAsset = generate(project, camera, date);
        return new ProjectSnapshotSelectionResult(date, snapshotAsset.bucket(), snapshotAsset.key(), snapshotAsset.mediaType());
    }

    private CameraSnapshotAsset generate(Project project, Camera camera, LocalDate date) {
        return generateCameraDailySnapshotUseCase.generate(project, camera, date, false);
    }

    private ProjectSnapshotMetadataResult toMetadata(CameraSnapshotAsset snapshot, java.time.Duration ttl, OffsetDateTime expiresAt) {
        return new ProjectSnapshotMetadataResult(
                snapshot.snapshotDate(),
                objectStorage.presign(snapshot.bucket(), snapshot.key(), ttl),
                expiresAt,
                snapshot.mediaType()
        );
    }

    private String detectMediaType(String key) {
        return ImageFormat.fromFileName(key)
                .orElse(ImageFormat.JPEG)
                .getMediaType();
    }
}

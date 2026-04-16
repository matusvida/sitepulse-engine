package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import com.sitepulse.engine.snapshot.application.usecase.RefreshCameraDailySnapshotsUseCase;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import com.sitepulse.engine.sync.domain.model.SyncJob;
import com.sitepulse.engine.sync.domain.event.ProjectSyncCompletedEvent;
import com.sitepulse.engine.sync.domain.port.ImageCatalogRepository;
import com.sitepulse.engine.sync.domain.port.SyncJobRepository;
import com.sitepulse.engine.sync.domain.port.SyncSource;
import com.sitepulse.engine.sync.domain.service.SyncFileParser;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunProjectSyncUseCase {

    private static final ZoneId DEFAULT_CAPTURE_ZONE = ZoneId.of("Europe/Bratislava");

    private final SyncSource syncSource;
    private final ObjectStorage objectStorage;
    private final SyncJobRepository syncJobRepository;
    private final ImageCatalogRepository imageCatalogRepository;
    private final CameraCatalogRepository cameraCatalogRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final RefreshCameraDailySnapshotsUseCase refreshCameraDailySnapshotsUseCase;
    private final SnapshotTimezoneResolver snapshotTimezoneResolver;

    private final SyncFileParser syncFileParser = new SyncFileParser();

    public void run(Project project) {
        var cameras = cameraCatalogRepository.findByProjectId(project.getId()).stream()
                .filter(this::isSyncable)
                .toList();
        log.info("Sync started for projectId={} cameraCount={}", project.getId(), cameras.size());
        SyncJob syncJob = syncJobRepository.save(SyncJob.start(project.getId(), OffsetDateTime.now(ZoneOffset.UTC)));

        try {
            for (Camera camera : cameras) {
                syncCamera(project, camera, syncJob);
            }
        } catch (SitePulseException ex) {
            log.error("Sync failed for projectId={} reason={}", project.getId(), ex.getMessage(), ex);
            syncJob.recordFatalFailure(safeMessage(ex));
        } catch (RuntimeException ex) {
            log.error("Unexpected sync failure for projectId={} reason={}", project.getId(), ex.getMessage(), ex);
            syncJob.recordFatalFailure(safeMessage(ex));
        }

        syncJob.finish(OffsetDateTime.now(ZoneOffset.UTC));
        syncJobRepository.save(syncJob);
        domainEventPublisher.publish(new ProjectSyncCompletedEvent(
                syncJob.getProjectId(),
                syncJob.getId(),
                syncJob.getStatus(),
                syncJob.getImagesFound(),
                syncJob.getImagesSynced()
        ));
        log.info("Sync finished for projectId={} status={} imagesFound={} imagesSynced={} errorCount={}",
                project.getId(),
                syncJob.getStatus(),
                syncJob.getImagesFound(),
                syncJob.getImagesSynced(),
                syncJob.getErrors().size());
    }

    private void syncCamera(Project project, Camera camera, SyncJob syncJob) {
        SyncCameraContext context = buildSyncCameraContext(project);
        ImportedSnapshotCandidate latestImportedToday = null;

        for (String folder : syncSource.listSubfolders(camera.getDropboxPath())) {
            LocalDate folderDate = syncFileParser.parseDateFolder(folder).orElse(null);
            if (folderDate == null) {
                continue;
            }
            latestImportedToday = syncFolder(project, camera, syncJob, context, folder, folderDate, latestImportedToday);
        }

        if (latestImportedToday != null) {
            refreshCameraDailySnapshotsUseCase.refresh(
                    project,
                    camera,
                    latestImportedToday.image(),
                    latestImportedToday.sourceBytes()
            );
        }
    }

    private ImportedSnapshotCandidate syncFolder(
            Project project,
            Camera camera,
            SyncJob syncJob,
            SyncCameraContext context,
            String folder,
            LocalDate folderDate,
            ImportedSnapshotCandidate latestImportedToday
    ) {
        ImportedSnapshotCandidate latestCandidate = latestImportedToday;
        for (SourceImageFile sourceImageFile : syncSource.listFiles(camera.getDropboxPath(), folder)) {
            latestCandidate = syncFile(project, camera, syncJob, context, folder, folderDate, sourceImageFile, latestCandidate);
        }
        return latestCandidate;
    }

    private ImportedSnapshotCandidate syncFile(
            Project project,
            Camera camera,
            SyncJob syncJob,
            SyncCameraContext context,
            String folder,
            LocalDate folderDate,
            SourceImageFile sourceImageFile,
            ImportedSnapshotCandidate latestImportedToday
    ) {
        syncJob.recordDiscoveredImage();
        ImageImport imageImport = toImageImport(project, camera, folder, folderDate, sourceImageFile);
        if (imageCatalogRepository.exists(imageImport.bucket(), imageImport.key())) {
            return latestImportedToday;
        }

        try {
            SyncImportResult importResult = importFile(camera, sourceImageFile, imageImport, context, latestImportedToday);
            if (!importResult.imported()) {
                log.info("Skipping duplicate image import for projectId={} cameraId={} bucket={} key={}",
                        project.getId(), camera.getId(), imageImport.bucket(), imageImport.key());
                return latestImportedToday;
            }
            syncJob.recordImportedImage();
            return importResult.snapshotCandidate() == null ? latestImportedToday : importResult.snapshotCandidate();
        } catch (SitePulseException ex) {
            log.warn("Failed to sync file for projectId={} cameraId={} file={} reason={}",
                    project.getId(), camera.getId(), sourceImageFile.name(), ex.getMessage());
            syncJob.recordFileFailure(sourceImageFile.name() + ": " + safeMessage(ex));
            return latestImportedToday;
        } catch (RuntimeException ex) {
            log.error("Unexpected sync failure for projectId={} cameraId={} file={}",
                    project.getId(), camera.getId(), sourceImageFile.name(), ex);
            syncJob.recordFileFailure(sourceImageFile.name() + ": " + safeMessage(ex));
            return latestImportedToday;
        }
    }

    private SyncImportResult importFile(
            Camera camera,
            SourceImageFile sourceImageFile,
            ImageImport imageImport,
            SyncCameraContext context,
            ImportedSnapshotCandidate latestImportedToday
    ) {
        if (shouldBufferForSnapshot(imageImport, context, latestImportedToday)) {
            return importWithBufferedBytes(camera, sourceImageFile, imageImport);
        }
        return importWithStreamingUpload(camera, sourceImageFile, imageImport);
    }

    private SyncImportResult importWithBufferedBytes(Camera camera, SourceImageFile sourceImageFile, ImageImport imageImport) {
        byte[] importedBytes = syncSource.downloadFile(camera.getDropboxPath(), sourceImageFile.path());
        objectStorage.upload(imageImport.bucket(), imageImport.key(), importedBytes, imageImport.contentType());
        ImageCatalogRepository.SaveImportedImageResult saveResult = imageCatalogRepository.saveImportedImage(imageImport);
        ImportedSnapshotCandidate snapshotCandidate = saveResult.image()
                .map(image -> new ImportedSnapshotCandidate(image, importedBytes))
                .orElse(null);
        return new SyncImportResult(saveResult.imported(), snapshotCandidate);
    }

    private SyncImportResult importWithStreamingUpload(Camera camera, SourceImageFile sourceImageFile, ImageImport imageImport) {
        try (InputStream inputStream = syncSource.downloadFileStream(camera.getDropboxPath(), sourceImageFile.path())) {
            objectStorage.upload(imageImport.bucket(), imageImport.key(), inputStream, sourceImageFile.sizeBytes(), imageImport.contentType());
        } catch (IOException ex) {
            throw new ExternalServiceException("Failed to close Dropbox file stream", ex);
        }
        ImageCatalogRepository.SaveImportedImageResult saveResult = imageCatalogRepository.saveImportedImage(imageImport);
        return new SyncImportResult(saveResult.imported(), null);
    }

    private ImageImport toImageImport(Project project, Camera camera, String folder, LocalDate folderDate, SourceImageFile sourceImageFile) {
        return new ImageImport(
                project.getId(),
                objectStorage.defaultBucket(),
                buildStorageKey(project, camera, folder, sourceImageFile.name()),
                syncFileParser.parseCapturedAt(sourceImageFile.name(), folderDate, resolveCaptureZone(project.getTimezone())),
                syncFileParser.contentType(sourceImageFile.name())
        );
    }

    private String buildStorageKey(Project project, Camera camera, String folder, String fileName) {
        StringBuilder key = new StringBuilder();
        appendSegment(key, project.getStorageKeyPrefix());
        appendSegment(key, camera.getKeyPrefix());
        appendSegment(key, folder);
        appendSegment(key, fileName);
        return key.toString();
    }

    private void appendSegment(StringBuilder key, String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }
        String normalized = segment.replaceAll("^/+", "").replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return;
        }
        if (key.length() > 0) {
            key.append('/');
        }
        key.append(normalized);
    }

    private boolean isSyncable(Camera camera) {
        return camera.getDropboxPath() != null && !camera.getDropboxPath().isBlank();
    }

    private String safeMessage(Throwable ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private ZoneId resolveCaptureZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_CAPTURE_ZONE;
        }
        return ZoneId.of(timezone);
    }

    private boolean isTodaySnapshotCandidate(ImageImport imageImport, LocalDate today, ZoneId snapshotZone) {
        return imageImport.capturedAt() != null
                && imageImport.capturedAt().atZoneSameInstant(snapshotZone).toLocalDate().equals(today);
    }

    private boolean shouldBufferForSnapshot(
            ImageImport imageImport,
            SyncCameraContext context,
            ImportedSnapshotCandidate latestImportedToday
    ) {
        return isTodaySnapshotCandidate(imageImport, context.today(), context.snapshotZone())
                && isNewerSnapshotCandidate(imageImport, latestImportedToday);
    }

    private boolean isNewerSnapshotCandidate(ImageImport imageImport, ImportedSnapshotCandidate currentCandidate) {
        if (imageImport.capturedAt() == null) {
            return false;
        }
        return currentCandidate == null
                || currentCandidate.image().getCapturedAt() == null
                || imageImport.capturedAt().isAfter(currentCandidate.image().getCapturedAt());
    }

    private SyncCameraContext buildSyncCameraContext(Project project) {
        ZoneId snapshotZone = snapshotTimezoneResolver.resolve(project);
        LocalDate today = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(snapshotZone).toLocalDate();
        return new SyncCameraContext(snapshotZone, today);
    }

    private record ImportedSnapshotCandidate(DetectionImage image, byte[] sourceBytes) {
    }

    private record SyncCameraContext(ZoneId snapshotZone, LocalDate today) {
    }

    private record SyncImportResult(boolean imported, ImportedSnapshotCandidate snapshotCandidate) {
    }
}

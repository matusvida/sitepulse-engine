package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import com.sitepulse.engine.sync.domain.model.SyncJob;
import com.sitepulse.engine.sync.domain.event.ProjectSyncCompletedEvent;
import com.sitepulse.engine.sync.domain.port.ImageCatalogRepository;
import com.sitepulse.engine.sync.domain.port.SyncJobRepository;
import com.sitepulse.engine.sync.domain.port.SyncSource;
import com.sitepulse.engine.sync.domain.service.SyncFileParser;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunProjectSyncUseCase {

    private final SyncSource syncSource;
    private final ObjectStorage objectStorage;
    private final SyncJobRepository syncJobRepository;
    private final ImageCatalogRepository imageCatalogRepository;
    private final CameraCatalogRepository cameraCatalogRepository;
    private final DomainEventPublisher domainEventPublisher;

    private final SyncFileParser syncFileParser = new SyncFileParser();

    public void run(Project project) {
        var cameras = cameraCatalogRepository.findByProjectId(project.getId()).stream()
                .filter(this::isSyncable)
                .toList();
        log.info("Sync started for projectId={} cameraCount={}", project.getId(), cameras.size());
        SyncJob syncJob = syncJobRepository.save(SyncJob.start(project.getId(), OffsetDateTime.now(ZoneOffset.UTC)));

        try {
            for (Camera camera : cameras) {
                for (String folder : syncSource.listSubfolders(camera.getDropboxPath())) {
                    LocalDate folderDate = syncFileParser.parseDateFolder(folder).orElse(null);
                    if (folderDate == null) {
                        continue;
                    }
                    for (SourceImageFile sourceImageFile : syncSource.listFiles(camera.getDropboxPath(), folder)) {
                        syncJob.recordDiscoveredImage();
                        ImageImport imageImport = toImageImport(project, camera, folder, folderDate, sourceImageFile);
                        if (imageCatalogRepository.exists(imageImport.bucket(), imageImport.key())) {
                            continue;
                        }
                        try {
                            byte[] bytes = syncSource.downloadFile(camera.getDropboxPath(), sourceImageFile.path());
                            objectStorage.upload(imageImport.bucket(), imageImport.key(), bytes, imageImport.contentType());
                            imageCatalogRepository.saveImportedImage(imageImport);
                            syncJob.recordImportedImage();
                        } catch (SitePulseException ex) {
                            log.warn("Failed to sync file for projectId={} cameraId={} file={} reason={}",
                                    project.getId(), camera.getId(), sourceImageFile.name(), ex.getMessage());
                            syncJob.recordFileFailure(sourceImageFile.name() + ": " + safeMessage(ex));
                        } catch (RuntimeException ex) {
                            log.error("Unexpected sync failure for projectId={} cameraId={} file={}",
                                    project.getId(), camera.getId(), sourceImageFile.name(), ex);
                            syncJob.recordFileFailure(sourceImageFile.name() + ": " + safeMessage(ex));
                        }
                    }
                }
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

    private ImageImport toImageImport(Project project, Camera camera, String folder, LocalDate folderDate, SourceImageFile sourceImageFile) {
        return new ImageImport(
                project.getId(),
                objectStorage.defaultBucket(),
                buildStorageKey(project, camera, folder, sourceImageFile.name()),
                syncFileParser.parseCapturedAt(sourceImageFile.name(), folderDate),
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
}

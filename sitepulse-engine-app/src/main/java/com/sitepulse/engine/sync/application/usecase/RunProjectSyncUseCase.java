package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import com.sitepulse.engine.sync.domain.model.SyncJob;
import com.sitepulse.engine.sync.domain.event.ProjectSyncCompletedEvent;
import com.sitepulse.engine.sync.domain.port.ImageCatalogRepository;
import com.sitepulse.engine.sync.domain.port.SyncJobRepository;
import com.sitepulse.engine.sync.domain.port.SyncSource;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunProjectSyncUseCase {

    private static final Pattern DATE_FOLDER = Pattern.compile("^(\\d{4})[-_]?(\\d{2})[-_]?(\\d{2})$");
    private static final Pattern FILE_TIMESTAMP = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})[_ ](\\d{2})[_:](\\d{2})[_:](\\d{2})");

    private final SyncSource syncSource;
    private final ObjectStorage objectStorage;
    private final SyncJobRepository syncJobRepository;
    private final ImageCatalogRepository imageCatalogRepository;
    private final DomainEventPublisher domainEventPublisher;

    public void run(Project project) {
        log.info("Sync started for projectId={} dropboxPath={}", project.getId(), project.getDropboxPath());
        SyncJob syncJob = syncJobRepository.save(SyncJob.start(project.getId(), OffsetDateTime.now(ZoneOffset.UTC)));

        try {
            for (String folder : syncSource.listSubfolders(project.getDropboxPath())) {
                LocalDate folderDate = parseDateFolder(folder);
                if (folderDate == null) {
                    continue;
                }
                for (SourceImageFile sourceImageFile : syncSource.listFiles(project.getDropboxPath(), folder)) {
                    syncJob.recordDiscoveredImage();
                    ImageImport imageImport = toImageImport(project.getId(), folder, folderDate, sourceImageFile);
                    if (imageCatalogRepository.exists(imageImport.bucket(), imageImport.key())) {
                        continue;
                    }
                    try {
                        byte[] bytes = syncSource.downloadFile(project.getDropboxPath(), sourceImageFile.path());
                        objectStorage.upload(imageImport.bucket(), imageImport.key(), bytes, imageImport.contentType());
                        imageCatalogRepository.saveImportedImage(imageImport);
                        syncJob.recordImportedImage();
                    } catch (SitePulseException ex) {
                        log.warn("Failed to sync file for projectId={} file={} reason={}", project.getId(), sourceImageFile.name(), ex.getMessage());
                        syncJob.recordFileFailure(sourceImageFile.name() + ": " + safeMessage(ex));
                    } catch (RuntimeException ex) {
                        log.error("Unexpected sync failure for projectId={} file={}", project.getId(), sourceImageFile.name(), ex);
                        syncJob.recordFileFailure(sourceImageFile.name() + ": " + safeMessage(ex));
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

    private ImageImport toImageImport(Integer projectId, String folder, LocalDate folderDate, SourceImageFile sourceImageFile) {
        return new ImageImport(
                projectId,
                objectStorage.defaultBucket(),
                folder + "/" + sourceImageFile.name(),
                parseCapturedAt(sourceImageFile.name(), folderDate),
                contentType(sourceImageFile.name())
        );
    }

    private LocalDate parseDateFolder(String folderName) {
        Matcher matcher = DATE_FOLDER.matcher(folderName);
        if (!matcher.matches()) {
            return null;
        }
        return LocalDate.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    private OffsetDateTime parseCapturedAt(String fileName, LocalDate folderDate) {
        Matcher matcher = FILE_TIMESTAMP.matcher(fileName);
        if (!matcher.find()) {
            return folderDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4)),
                Integer.parseInt(matcher.group(5)),
                Integer.parseInt(matcher.group(6)),
                0,
                ZoneOffset.UTC
        );
    }

    private String contentType(String fileName) {
        return fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private String safeMessage(Throwable ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}

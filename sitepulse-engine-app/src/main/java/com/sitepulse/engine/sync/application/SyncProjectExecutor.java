package com.sitepulse.engine.sync.application;

import com.sitepulse.engine.integration.dropbox.DropboxClientService;
import com.sitepulse.engine.integration.dropbox.DropboxClientService.DropboxFileEntry;
import com.sitepulse.engine.integration.storage.StorageService;
import com.sitepulse.engine.project.domain.ProjectEntity;
import com.sitepulse.engine.sync.domain.SyncJobEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncProjectExecutor {

    private static final Pattern DATE_FOLDER = Pattern.compile("^(\\d{4})[-_]?(\\d{2})[-_]?(\\d{2})$");
    private static final Pattern FILE_TIMESTAMP = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})[_ ](\\d{2})[_:](\\d{2})[_:](\\d{2})");

    private final DropboxClientService dropboxClientService;
    private final StorageService storageService;
    private final SyncJobPersistenceService syncJobPersistenceService;
    private final SyncImagePersistenceService syncImagePersistenceService;

    public void syncProject(ProjectEntity project) {
        log.info("Sync started for projectId={} dropboxPath={}", project.getId(), project.getDropboxPath());
        SyncJobEntity job = syncJobPersistenceService.createRunningJob(project.getId());

        int imagesFound = 0;
        int imagesSynced = 0;
        List<String> errors = new ArrayList<>();
        String status = "DONE";
        try {
            for (String folder : dropboxClientService.listSubfolders(project.getDropboxPath())) {
                LocalDate folderDate = parseDateFolder(folder);
                if (folderDate == null) {
                    continue;
                }
                for (DropboxFileEntry file : dropboxClientService.listFiles(project.getDropboxPath(), folder)) {
                    imagesFound++;
                    String key = folder + "/" + file.name();
                    if (syncImagePersistenceService.exists(storageService.defaultBucket(), key)) {
                        continue;
                    }
                    try {
                        byte[] bytes = dropboxClientService.downloadFile(project.getDropboxPath(), file.path());
                        storageService.upload(storageService.defaultBucket(), key, bytes, contentType(file.name()));
                        syncImagePersistenceService.saveSyncedImage(
                                storageService.defaultBucket(),
                                key,
                                project.getId(),
                                parseCapturedAt(file.name(), folderDate)
                        );
                        imagesSynced++;
                    } catch (Exception ex) {
                        log.warn("Failed to sync file for projectId={} file={} reason={}", project.getId(), file.name(), ex.getMessage());
                        errors.add(file.name() + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            status = "FAILED";
            log.error("Sync failed for projectId={} reason={}", project.getId(), ex.getMessage(), ex);
            errors.add(ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage());
        }

        if (!"FAILED".equals(status) && imagesFound == 0 && !errors.isEmpty()) {
            status = "FAILED";
        }
        syncJobPersistenceService.finishJob(job.getId(), status, imagesFound, imagesSynced, errors);
        log.info("Sync finished for projectId={} status={} imagesFound={} imagesSynced={} errorCount={}",
                project.getId(), status, imagesFound, imagesSynced, errors.size());
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
}

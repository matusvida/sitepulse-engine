package com.sitepulse.engine.sync.domain.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class SyncJob {

    private final Integer id;
    private final Integer projectId;
    private SyncJobStatus status;
    private int imagesFound;
    private int imagesSynced;
    private final List<String> errors;
    private final OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;

    private SyncJob(
            Integer id,
            Integer projectId,
            SyncJobStatus status,
            int imagesFound,
            int imagesSynced,
            List<String> errors,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.status = status;
        this.imagesFound = imagesFound;
        this.imagesSynced = imagesSynced;
        this.errors = new ArrayList<>(errors);
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public static SyncJob start(Integer projectId, OffsetDateTime startedAt) {
        return new SyncJob(null, projectId, SyncJobStatus.RUNNING, 0, 0, List.of(), startedAt, null);
    }

    public static SyncJob restore(
            Integer id,
            Integer projectId,
            SyncJobStatus status,
            int imagesFound,
            int imagesSynced,
            List<String> errors,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
        return new SyncJob(id, projectId, status, imagesFound, imagesSynced, errors, startedAt, finishedAt);
    }

    public SyncJob persisted(Integer id) {
        return new SyncJob(id, projectId, status, imagesFound, imagesSynced, errors, startedAt, finishedAt);
    }

    public void recordDiscoveredImage() {
        imagesFound++;
    }

    public void recordImportedImage() {
        imagesSynced++;
    }

    public void recordFileFailure(String message) {
        errors.add(message);
    }

    public void recordFatalFailure(String message) {
        errors.add(message);
        status = SyncJobStatus.FAILED;
    }

    public void finish(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
        if (status != SyncJobStatus.FAILED) {
            status = imagesFound == 0 && !errors.isEmpty() ? SyncJobStatus.FAILED : SyncJobStatus.DONE;
        }
    }

    public String errorSummary() {
        return errors.isEmpty() ? null : String.join("; ", errors);
    }
}

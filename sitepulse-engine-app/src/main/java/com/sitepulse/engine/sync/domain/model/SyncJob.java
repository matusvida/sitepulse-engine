package com.sitepulse.engine.sync.domain.model;

import com.sitepulse.engine.sync.domain.enums.SyncJobStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SyncJob {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final Integer id;

    private final Integer projectId;

    @ToString.Include
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
        requireRunning();
        imagesFound++;
    }

    public void recordImportedImage() {
        requireRunning();
        imagesSynced++;
    }

    public void recordFileFailure(String message) {
        errors.add(message);
    }

    public void recordFatalFailure(String message) {
        if (status == SyncJobStatus.DONE) {
            throw new IllegalStateException("Cannot record fatal failure on a completed sync job");
        }
        errors.add(message);
        status = SyncJobStatus.FAILED;
    }

    public void finish(OffsetDateTime finishedAt) {
        if (status == SyncJobStatus.DONE) {
            throw new IllegalStateException("Sync job is already finished");
        }
        this.finishedAt = finishedAt;
        if (status != SyncJobStatus.FAILED) {
            status = imagesFound == 0 && !errors.isEmpty() ? SyncJobStatus.FAILED : SyncJobStatus.DONE;
        }
    }

    public String errorSummary() {
        return errors.isEmpty() ? null : String.join("; ", errors);
    }

    private void requireRunning() {
        if (status != SyncJobStatus.RUNNING) {
            throw new IllegalStateException("Sync job is not in RUNNING state, current: " + status);
        }
    }
}

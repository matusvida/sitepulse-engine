package com.sitepulse.engine.sync.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyncJobTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void startCreatesRunningJob() {
        SyncJob job = SyncJob.start(1, NOW);
        assertEquals(SyncJobStatus.RUNNING, job.getStatus());
        assertEquals(0, job.getImagesFound());
        assertEquals(0, job.getImagesSynced());
        assertTrue(job.getErrors().isEmpty());
    }

    @Test
    void recordDiscoveredImageIncrementsCounter() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordDiscoveredImage();
        job.recordDiscoveredImage();
        assertEquals(2, job.getImagesFound());
    }

    @Test
    void recordImportedImageIncrementsCounter() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordImportedImage();
        assertEquals(1, job.getImagesSynced());
    }

    @Test
    void recordDiscoveredImageRejectsNonRunning() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFatalFailure("fatal");
        assertThrows(IllegalStateException.class, job::recordDiscoveredImage);
    }

    @Test
    void recordImportedImageRejectsNonRunning() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFatalFailure("fatal");
        assertThrows(IllegalStateException.class, job::recordImportedImage);
    }

    @Test
    void recordFileFailureAddsError() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFileFailure("file1.jpg: timeout");
        assertEquals(1, job.getErrors().size());
        assertEquals("file1.jpg: timeout", job.getErrors().get(0));
    }

    @Test
    void recordFatalFailureSetsFailedStatus() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFatalFailure("connection lost");
        assertEquals(SyncJobStatus.FAILED, job.getStatus());
        assertEquals(1, job.getErrors().size());
    }

    @Test
    void recordFatalFailureRejectsDoneJob() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordDiscoveredImage();
        job.recordImportedImage();
        job.finish(NOW);
        assertEquals(SyncJobStatus.DONE, job.getStatus());
        assertThrows(IllegalStateException.class, () -> job.recordFatalFailure("too late"));
    }

    @Test
    void finishSetsDoneWhenImagesExist() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordDiscoveredImage();
        job.recordImportedImage();
        job.finish(NOW.plusMinutes(5));
        assertEquals(SyncJobStatus.DONE, job.getStatus());
        assertNotNull(job.getFinishedAt());
    }

    @Test
    void finishSetsFailedWhenNoImagesButErrors() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFileFailure("error");
        job.finish(NOW.plusMinutes(5));
        assertEquals(SyncJobStatus.FAILED, job.getStatus());
    }

    @Test
    void finishPreservesFailedStatus() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFatalFailure("fatal error");
        job.finish(NOW.plusMinutes(5));
        assertEquals(SyncJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getFinishedAt());
    }

    @Test
    void finishRejectsAlreadyDone() {
        SyncJob job = SyncJob.start(1, NOW);
        job.finish(NOW);
        assertThrows(IllegalStateException.class, () -> job.finish(NOW.plusMinutes(1)));
    }

    @Test
    void errorSummaryJoinsErrors() {
        SyncJob job = SyncJob.start(1, NOW);
        job.recordFileFailure("a");
        job.recordFileFailure("b");
        assertEquals("a; b", job.errorSummary());
    }

    @Test
    void errorSummaryReturnsNullWhenEmpty() {
        SyncJob job = SyncJob.start(1, NOW);
        assertNull(job.errorSummary());
    }
}

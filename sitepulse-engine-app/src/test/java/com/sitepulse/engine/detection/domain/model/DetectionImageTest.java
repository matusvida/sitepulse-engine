package com.sitepulse.engine.detection.domain.model;

import com.sitepulse.engine.detection.domain.enums.ImageStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetectionImageTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void createNewSetsStatusNew() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        assertEquals(ImageStatus.NEW, image.getStatus());
    }

    @Test
    void markProcessingFromNewSucceeds() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        image.markProcessing(NOW.plusMinutes(1));
        assertEquals(ImageStatus.PROCESSING, image.getStatus());
    }

    @Test
    void markProcessingFromProcessingFails() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        image.markProcessing(NOW.plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> image.markProcessing(NOW.plusMinutes(2)));
    }

    @Test
    void markDoneFromProcessingSucceeds() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        image.markProcessing(NOW.plusMinutes(1));
        image.markDone(NOW.plusMinutes(2));
        assertEquals(ImageStatus.DONE, image.getStatus());
    }

    @Test
    void markDoneFromNewFails() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        assertThrows(IllegalStateException.class, () -> image.markDone(NOW.plusMinutes(1)));
    }

    @Test
    void markFailedFromProcessingSucceeds() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        image.markProcessing(NOW.plusMinutes(1));
        image.markFailed(NOW.plusMinutes(2));
        assertEquals(ImageStatus.FAILED, image.getStatus());
    }

    @Test
    void markFailedFromNewFails() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        assertThrows(IllegalStateException.class, () -> image.markFailed(NOW.plusMinutes(1)));
    }

    @Test
    void markFailedFromDoneSucceeds() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        image.markProcessing(NOW.plusMinutes(1));
        image.markDone(NOW.plusMinutes(2));
        image.markFailed(NOW.plusMinutes(3));
        assertEquals(ImageStatus.FAILED, image.getStatus());
    }

    @Test
    void markFailedFromFailedIsIdempotent() {
        DetectionImage image = DetectionImage.createNew("bucket", "key", 1, 1, NOW, NOW);
        image.markProcessing(NOW.plusMinutes(1));
        image.markFailed(NOW.plusMinutes(2));
        image.markFailed(NOW.plusMinutes(3));
        assertEquals(ImageStatus.FAILED, image.getStatus());
    }

    @Test
    void createDetectedSetsStatusDone() {
        DetectionImage image = DetectionImage.createDetected("bucket", "key", 1, 1, NOW, NOW);
        assertEquals(ImageStatus.DONE, image.getStatus());
    }
}

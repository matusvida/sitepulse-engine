package com.sitepulse.engine.detection.domain.model;

import com.sitepulse.engine.detection.domain.ImageStatus;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class DetectionImage {

    private final Integer id;
    private final String bucket;
    private final String key;
    private ImageStatus status;
    private final Integer projectId;
    private final Integer cameraId;
    private final OffsetDateTime capturedAt;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private DetectionImage(
            Integer id,
            String bucket,
            String key,
            ImageStatus status,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.bucket = bucket;
        this.key = key;
        this.status = status;
        this.projectId = projectId;
        this.cameraId = cameraId;
        this.capturedAt = capturedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DetectionImage restore(
            Integer id,
            String bucket,
            String key,
            ImageStatus status,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new DetectionImage(id, bucket, key, status, projectId, cameraId, capturedAt, createdAt, updatedAt);
    }

    public static DetectionImage createDetected(
            String bucket,
            String key,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime now
    ) {
        return new DetectionImage(null, bucket, key, ImageStatus.DONE, projectId, cameraId, capturedAt, now, now);
    }

    public void markDone(OffsetDateTime now) {
        status = ImageStatus.DONE;
        updatedAt = now;
    }

    public void markFailed(OffsetDateTime now) {
        status = ImageStatus.FAILED;
        updatedAt = now;
    }
}

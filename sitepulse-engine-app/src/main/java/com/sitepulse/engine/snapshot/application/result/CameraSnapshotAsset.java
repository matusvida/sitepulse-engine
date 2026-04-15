package com.sitepulse.engine.snapshot.application.result;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CameraSnapshotAsset(
        Integer cameraId,
        LocalDate snapshotDate,
        Integer sourceImageId,
        String bucket,
        String key,
        String mediaType,
        boolean frozen,
        OffsetDateTime generatedAt
) {
}

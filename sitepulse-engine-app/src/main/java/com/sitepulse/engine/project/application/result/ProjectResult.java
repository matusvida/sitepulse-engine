package com.sitepulse.engine.project.application.result;

import java.time.OffsetDateTime;

public record ProjectResult(
        Integer id,
        String name,
        String location,
        int imageCount,
        int cameraCount,
        String latestSnapshotAt,
        String storageKeyPrefix,
        OffsetDateTime createdAt
) {
}

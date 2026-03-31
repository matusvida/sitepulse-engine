package com.sitepulse.engine.sync.domain.model;

import java.time.OffsetDateTime;

public record ImageImport(
        Integer projectId,
        String bucket,
        String key,
        OffsetDateTime capturedAt,
        String contentType
) {
}

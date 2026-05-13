package com.sitepulse.engine.report.domain.model;

import java.time.OffsetDateTime;

public record ReportImageEvidence(
        Integer imageId,
        String date,
        String base64Content,
        OffsetDateTime capturedAt,
        String bucket,
        String key,
        String url
) {
}

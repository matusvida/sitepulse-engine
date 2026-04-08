package com.sitepulse.engine.project.application.result;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectSnapshotMetadataResult(
        LocalDate date,
        String url,
        OffsetDateTime expiresAt,
        String mediaType
) {
}

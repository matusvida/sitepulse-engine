package com.sitepulse.engine.project.application.result;

import java.time.LocalDate;

public record ProjectSnapshotSelectionResult(
        LocalDate date,
        String bucket,
        String key,
        String mediaType
) {
}

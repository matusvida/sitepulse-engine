package com.sitepulse.engine.project.application.result;

import com.sitepulse.engine.detection.domain.model.StoredImage;
import java.time.LocalDate;

public record ProjectSnapshotSelectionResult(
        LocalDate date,
        StoredImage image,
        String mediaType
) {
}

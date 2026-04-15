package com.sitepulse.engine.snapshot.application.result;

import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import java.util.List;

public record CameraSnapshotSourceDecision(
        List<ImageEntity> sourceImages,
        boolean frozen
) {
}

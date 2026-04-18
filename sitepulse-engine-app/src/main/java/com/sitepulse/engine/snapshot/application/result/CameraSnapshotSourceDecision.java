package com.sitepulse.engine.snapshot.application.result;

import com.sitepulse.engine.detection.domain.model.StoredImage;
import java.util.List;

public record CameraSnapshotSourceDecision(
        List<StoredImage> sourceImages,
        boolean frozen
) {
}

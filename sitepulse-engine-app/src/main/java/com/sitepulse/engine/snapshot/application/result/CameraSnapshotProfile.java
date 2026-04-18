package com.sitepulse.engine.snapshot.application.result;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import java.time.LocalTime;

public record CameraSnapshotProfile(
        Integer cameraId,
        Integer targetWidth,
        Integer targetQuality,
        ImageFormat targetFormat,
        LocalTime freezeTime
) {
}

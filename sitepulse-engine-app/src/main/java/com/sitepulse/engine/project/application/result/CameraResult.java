package com.sitepulse.engine.project.application.result;

import java.time.OffsetDateTime;
import java.util.List;

public record CameraResult(
        Integer id,
        Integer projectId,
        String name,
        String dropboxPath,
        List<List<Double>> roiPolygon,
        Boolean dropOutside,
        String keyPrefix,
        OffsetDateTime createdAt
) {
}

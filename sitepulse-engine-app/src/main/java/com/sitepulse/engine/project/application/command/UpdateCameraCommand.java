package com.sitepulse.engine.project.application.command;

import java.util.List;

public record UpdateCameraCommand(
        Integer projectId,
        Integer cameraId,
        String dropboxPath,
        String keyPrefix,
        List<List<Double>> roiPolygon,
        Boolean dropOutside
) {
}

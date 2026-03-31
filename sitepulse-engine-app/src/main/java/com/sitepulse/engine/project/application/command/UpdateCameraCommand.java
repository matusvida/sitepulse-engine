package com.sitepulse.engine.project.application.command;

import java.util.List;

public record UpdateCameraCommand(
        Integer projectId,
        Integer cameraId,
        List<List<Double>> roiPolygon,
        Boolean dropOutside
) {
}

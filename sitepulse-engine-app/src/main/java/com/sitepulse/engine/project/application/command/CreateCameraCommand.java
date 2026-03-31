package com.sitepulse.engine.project.application.command;

import java.util.List;

public record CreateCameraCommand(
        Integer projectId,
        String name,
        String keyPrefix,
        List<List<Double>> roiPolygon,
        Boolean dropOutside
) {
}

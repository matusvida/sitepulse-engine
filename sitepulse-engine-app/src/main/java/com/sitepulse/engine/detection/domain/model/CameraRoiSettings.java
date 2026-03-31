package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record CameraRoiSettings(List<List<Double>> roiPolygon, boolean dropOutside) {
}

package com.sitepulse.engine.detection.domain.model;

import java.util.List;

public record DetectedObject(Integer classId, String className, Double score, List<Double> bboxXyxy, Boolean inRoi) {
}

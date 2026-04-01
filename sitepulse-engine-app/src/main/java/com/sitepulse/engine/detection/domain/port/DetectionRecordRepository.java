package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectedObject;
import java.util.List;

public interface DetectionRecordRepository {

    void replaceDetections(Integer imageId, Integer projectId, String modelVersion, List<DetectedObject> detections);
}

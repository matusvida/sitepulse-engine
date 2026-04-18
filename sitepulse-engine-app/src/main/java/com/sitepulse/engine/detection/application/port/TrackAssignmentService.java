package com.sitepulse.engine.detection.application.port;

import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import java.util.List;

public interface TrackAssignmentService {

    List<DetectedObject> assignTracks(DetectionImage image, List<DetectedObject> detections);
}

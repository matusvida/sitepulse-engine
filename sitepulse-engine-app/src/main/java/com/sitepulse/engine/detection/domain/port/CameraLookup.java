package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;

public interface CameraLookup {

    Integer findCameraIdByProjectAndKey(Integer projectId, String key);

    CameraRoiSettings findRoiSettings(Integer projectId, String key);
}

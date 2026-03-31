package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import java.util.List;
import java.util.Optional;

public interface DetectionImageRepository {

    List<DetectionImage> claimPendingImages(int limit);

    Optional<DetectionImage> findById(Integer imageId);

    DetectionImage save(DetectionImage image);
}

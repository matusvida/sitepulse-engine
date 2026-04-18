package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import java.util.List;
import java.util.Optional;

public interface DetectionImageRepository {

    List<DetectionImage> claimPendingImages(int limit);

    Optional<DetectionImage> findById(Integer imageId);

    Optional<DetectionImage> findPreviousDone(DetectionImage image);

    boolean existsByBucketAndKey(String bucket, String key);

    DetectionImage save(DetectionImage image);
}

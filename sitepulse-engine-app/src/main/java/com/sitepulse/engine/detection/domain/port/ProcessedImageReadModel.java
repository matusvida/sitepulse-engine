package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcessedImageReadModel {

    List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId);

    List<StoredImage> findRepresentativeSnapshots(Integer projectId);

    Optional<StoredImage> findClosestSnapshot(Integer projectId, OffsetDateTime dayStart, OffsetDateTime dayEnd, OffsetDateTime midday);

    Optional<StoredImage> findPreviousDoneImage(Integer projectId, Integer cameraId, OffsetDateTime capturedAt, Integer imageId);

    List<StoredImage> findDoneInRange(Integer projectId, OffsetDateTime from, OffsetDateTime to);

    List<StoredImage> findProcessedByProject(Integer projectId);

    List<DetectedObject> findDetections(Integer imageId);
}

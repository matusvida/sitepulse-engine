package com.sitepulse.engine.snapshot.domain.port;

import com.sitepulse.engine.detection.domain.model.StoredImage;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface SnapshotSourceImageReadModel {

    List<StoredImage> findRepresentativeSnapshotCandidatesByCameraId(
            Integer cameraId,
            OffsetDateTime dayStart,
            OffsetDateTime dayEnd,
            OffsetDateTime midday,
            int limit
    );

    List<StoredImage> findLatestSnapshotCandidatesByCameraId(
            Integer cameraId,
            OffsetDateTime dayStart,
            OffsetDateTime dayEnd,
            int limit
    );

    List<LocalDate> findAvailableSnapshotDatesByCameraId(Integer cameraId, String timezone);
}

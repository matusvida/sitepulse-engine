package com.sitepulse.engine.project.domain.port;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ProjectReadModel {

    int countCameras(Integer projectId);

    Optional<OffsetDateTime> latestSnapshotAt(Integer projectId);
}

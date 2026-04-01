package com.sitepulse.engine.sync.domain.port;

import com.sitepulse.engine.sync.domain.model.SyncJob;
import java.util.Optional;

public interface SyncJobRepository {

    SyncJob save(SyncJob syncJob);

    Optional<SyncJob> findLatestForProject(Integer projectId);
}

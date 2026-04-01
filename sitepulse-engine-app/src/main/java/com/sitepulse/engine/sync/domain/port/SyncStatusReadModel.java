package com.sitepulse.engine.sync.domain.port;

import com.sitepulse.engine.sync.application.result.SyncStatusResult;

public interface SyncStatusReadModel {

    SyncStatusResult getLatestStatus(Integer projectId);
}

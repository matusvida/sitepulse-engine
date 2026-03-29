package com.sitepulse.engine.sync.persistence;

import com.sitepulse.engine.sync.domain.SyncJobEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobRepository extends JpaRepository<SyncJobEntity, Integer> {

    Optional<SyncJobEntity> findTopByProjectIdOrderByStartedAtDesc(Integer projectId);
}

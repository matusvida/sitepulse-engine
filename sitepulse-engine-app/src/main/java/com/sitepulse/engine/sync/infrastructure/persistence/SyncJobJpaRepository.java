package com.sitepulse.engine.sync.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobJpaRepository extends JpaRepository<SyncJobJpaEntity, Integer> {

    Optional<SyncJobJpaEntity> findTopByProjectIdOrderByStartedAtDesc(Integer projectId);
}

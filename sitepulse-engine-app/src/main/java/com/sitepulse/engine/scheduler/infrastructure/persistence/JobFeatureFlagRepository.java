package com.sitepulse.engine.scheduler.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobFeatureFlagRepository extends JpaRepository<JobFeatureFlagEntity, String> {
}

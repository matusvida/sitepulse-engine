package com.sitepulse.engine.project.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Integer> {
}

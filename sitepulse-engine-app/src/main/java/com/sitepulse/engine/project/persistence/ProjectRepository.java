package com.sitepulse.engine.project.persistence;

import com.sitepulse.engine.project.domain.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Integer> {
}

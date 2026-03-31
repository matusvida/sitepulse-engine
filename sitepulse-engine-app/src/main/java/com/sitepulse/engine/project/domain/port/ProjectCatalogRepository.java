package com.sitepulse.engine.project.domain.port;

import com.sitepulse.engine.project.domain.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectCatalogRepository {

    List<Project> findAll();

    Optional<Project> findById(Integer projectId);

    Project save(Project project);
}

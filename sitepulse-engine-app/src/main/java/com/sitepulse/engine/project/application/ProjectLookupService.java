package com.sitepulse.engine.project.application;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectLookupService {

    private final ProjectCatalogRepository projectCatalogRepository;

    public Project requireProject(Integer projectId) {
        return projectCatalogRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
}

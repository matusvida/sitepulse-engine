package com.sitepulse.engine.project.application;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectLookupService {

    private final ProjectCatalogRepository projectCatalogRepository;

    public Project requireProject(Integer projectId) {
        return projectCatalogRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}

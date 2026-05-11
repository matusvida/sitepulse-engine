package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.ProjectResultMapper;
import com.sitepulse.engine.project.application.result.ProjectResult;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectsQuery {

    private final ProjectCatalogRepository projectCatalogRepository;
    private final ProjectResultMapper projectResultMapper;

    public List<ProjectResult> get() {
        return projectCatalogRepository.findAll().stream().map(projectResultMapper::toResult).toList();
    }

    public ProjectResult toResult(com.sitepulse.engine.project.domain.model.Project project) {
        return projectResultMapper.toResult(project);
    }
}

package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.ProjectResultMapper;
import com.sitepulse.engine.project.application.result.ProjectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectQuery {

    private final ProjectLookupService projectLookupService;
    private final ProjectResultMapper projectResultMapper;

    public ProjectResult get(Integer projectId) {
        return projectResultMapper.toResult(projectLookupService.requireProject(projectId));
    }
}

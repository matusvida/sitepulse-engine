package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.ProjectResultMapper;
import com.sitepulse.engine.project.application.result.CameraResult;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectCamerasQuery {

    private final ProjectLookupService projectLookupService;
    private final CameraCatalogRepository cameraCatalogRepository;
    private final ProjectResultMapper projectResultMapper;

    public List<CameraResult> get(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return cameraCatalogRepository.findByProjectId(projectId).stream().map(projectResultMapper::toResult).toList();
    }
}

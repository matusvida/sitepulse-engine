package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectReportsQuery {

    private final ProjectLookupService projectLookupService;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final ReportResultMapper reportResultMapper;

    public List<ProgressReportResult> list(Integer projectId, int limit, int offset) {
        projectLookupService.requireProject(projectId);
        return progressReportCatalogRepository.findByProject(projectId, limit, offset).stream()
                .map(reportResultMapper::toResult)
                .toList();
    }
}

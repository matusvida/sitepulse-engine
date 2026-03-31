package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectReportQuery {

    private final ProjectLookupService projectLookupService;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final ReportResultMapper reportResultMapper;

    public ProgressReportResult get(Integer projectId, Integer reportId) {
        projectLookupService.requireProject(projectId);
        return progressReportCatalogRepository.findByIdAndProject(reportId, projectId)
                .map(reportResultMapper::toResult)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report not found"));
    }
}

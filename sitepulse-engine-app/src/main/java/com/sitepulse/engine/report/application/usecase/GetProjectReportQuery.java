package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.service.ReportEvidenceQueryService;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectReportQuery {

    private final ProjectLookupService projectLookupService;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final ReportResultMapper reportResultMapper;
    private final ReportEvidenceQueryService reportEvidenceQueryService;

    public ProgressReportResult get(Integer projectId, Integer reportId) {
        projectLookupService.requireProject(projectId);
        return progressReportCatalogRepository.findByIdAndProject(reportId, projectId)
                .map(report -> {
                    ProgressReportResult result = reportResultMapper.toResult(report);
                    int evidenceCount = report.getEvidenceImageCount() == null ? 0 : report.getEvidenceImageCount();
                    result.setEvidenceImages(reportEvidenceQueryService.list(
                            projectId,
                            report.getDateRangeStart(),
                            report.getDateRangeEnd(),
                            evidenceCount
                    ));
                    return result;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }
}

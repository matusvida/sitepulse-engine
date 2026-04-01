package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.port.ReportReadModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectReportsQuery {

    private final ProjectLookupService projectLookupService;
    private final ReportReadModel reportReadModel;

    public List<ProgressReportResult> list(Integer projectId, int limit, int offset) {
        projectLookupService.requireProject(projectId);
        return reportReadModel.findByProject(projectId, limit, offset);
    }
}

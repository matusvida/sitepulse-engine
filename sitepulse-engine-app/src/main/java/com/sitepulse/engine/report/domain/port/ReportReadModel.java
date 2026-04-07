package com.sitepulse.engine.report.domain.port;

import com.sitepulse.engine.report.application.result.ProgressReportResult;
import java.util.List;

public interface ReportReadModel {

    List<ProgressReportResult> findByProject(Integer projectId, int limit, int offset);
}

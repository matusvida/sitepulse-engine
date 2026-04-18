package com.sitepulse.engine.report.domain.port;

import com.sitepulse.engine.report.domain.model.ProgressReport;
import java.util.List;

public interface ReportReadModel {

    List<ProgressReport> findByProject(Integer projectId, int limit, int offset);
}

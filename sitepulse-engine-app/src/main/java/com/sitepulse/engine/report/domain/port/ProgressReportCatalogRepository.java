package com.sitepulse.engine.report.domain.port;

import com.sitepulse.engine.report.domain.model.ProgressReport;
import java.util.List;
import java.util.Optional;

public interface ProgressReportCatalogRepository {

    ProgressReport save(ProgressReport report);

    List<ProgressReport> findByProject(Integer projectId, int limit, int offset);

    Optional<ProgressReport> findByIdAndProject(Integer reportId, Integer projectId);
}

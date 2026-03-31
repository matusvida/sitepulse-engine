package com.sitepulse.engine.report.domain.port;

import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import java.time.LocalDate;
import java.util.List;

public interface ReportEvidenceImageProvider {

    List<ReportImageEvidence> gather(Integer projectId, LocalDate dateFrom, LocalDate dateTo, int maxImages);
}

package com.sitepulse.engine.report.domain.port;

import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import java.util.List;

public interface ReportGenerator {

    String generate(List<ReportImageEvidence> imageData, String metricsContext, String milestonesContext);
}

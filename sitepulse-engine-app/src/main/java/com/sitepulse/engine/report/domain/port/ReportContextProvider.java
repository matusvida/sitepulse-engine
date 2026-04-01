package com.sitepulse.engine.report.domain.port;

public interface ReportContextProvider {

    String getMetricsSummary(Integer projectId, int days);

    String getMilestoneSummary(Integer projectId);
}

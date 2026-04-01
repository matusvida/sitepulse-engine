package com.sitepulse.engine.report.domain.model;

import java.time.LocalDate;

public record ReportPeriod(LocalDate dateFrom, LocalDate dateTo) {
    public ReportPeriod {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }
    }
}

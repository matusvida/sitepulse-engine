package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportSummary(
        LocalDate weekStart,
        int imageCount,
        int activeDays,
        WeatherSummary weatherPattern,
        ConfidenceLevel confidenceLevel,
        List<String> notableEvents,
        String contextText
) {
}

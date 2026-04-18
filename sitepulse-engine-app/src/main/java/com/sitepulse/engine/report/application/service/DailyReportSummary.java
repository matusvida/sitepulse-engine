package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.time.LocalDate;
import java.util.List;

public record DailyReportSummary(
        LocalDate date,
        int imageCount,
        WeatherSummary weatherSummary,
        ConfidenceLevel confidenceLevel,
        List<String> dominantClasses,
        List<String> notableEvents,
        String contextText
) {
}

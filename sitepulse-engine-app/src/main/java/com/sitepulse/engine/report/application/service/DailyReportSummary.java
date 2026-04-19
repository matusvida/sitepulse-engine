package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.time.LocalDate;
import java.util.List;

public record DailyReportSummary(
        LocalDate date,
        int imageCount,
        DailyActivityStatus activityStatus,
        WeatherSummary weatherSummary,
        ConfidenceLevel confidenceLevel,
        List<String> dominantClasses,
        List<String> notableEvents,
        String contextText
) {
}

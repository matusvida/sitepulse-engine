package com.sitepulse.engine.metrics.domain.model;

import com.sitepulse.engine.metrics.domain.enums.DailyActivityReasonCode;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.DailyObservationConfidence;
import com.sitepulse.engine.metrics.domain.enums.DailyWeatherStatus;
import java.util.List;

public record DailyActivityAssessment(
        DailyActivityStatus activityStatus,
        DailyObservationConfidence activityConfidence,
        DailyWeatherStatus weatherStatus,
        boolean weatherImpacted,
        List<DailyActivityReasonCode> reasonCodes,
        String summaryNote
) {
}

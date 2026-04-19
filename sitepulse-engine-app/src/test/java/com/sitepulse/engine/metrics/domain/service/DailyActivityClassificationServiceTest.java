package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.DailyWeatherStatus;
import com.sitepulse.engine.metrics.domain.model.DailyActivityAssessment;
import com.sitepulse.engine.metrics.domain.model.DailyActivityEvidence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyActivityClassificationServiceTest {

    private final DailyActivityClassificationService service = new DailyActivityClassificationService();

    @Test
    void classifiesMovementAsActive() {
        DailyActivityAssessment assessment = service.classify(new DailyActivityEvidence(
                5,
                2,
                3,
                2.0,
                9.0,
                3.0,
                4.0,
                3,
                true,
                true,
                false,
                false,
                false
        ));

        assertEquals(DailyActivityStatus.ACTIVE, assessment.activityStatus());
        assertEquals(DailyWeatherStatus.CLEAR_OR_NORMAL, assessment.weatherStatus());
        assertTrue(assessment.reasonCodes().stream().anyMatch(code -> code.toPersistenceValue().equals("movement_signals_present")));
    }

    @Test
    void classifiesLowCoverageAsUnknown() {
        DailyActivityAssessment assessment = service.classify(new DailyActivityEvidence(
                1,
                0,
                0,
                0.0,
                1.0,
                0.0,
                1.0,
                0,
                false,
                false,
                true,
                false,
                true
        ));

        assertEquals(DailyActivityStatus.UNKNOWN, assessment.activityStatus());
        assertEquals(DailyWeatherStatus.RAIN, assessment.weatherStatus());
    }
}

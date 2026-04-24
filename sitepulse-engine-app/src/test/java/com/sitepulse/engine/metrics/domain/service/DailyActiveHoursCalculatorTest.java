package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.detection.application.enums.ImageEvidenceCandidateTag;
import com.sitepulse.engine.detection.domain.model.ImageEvidenceSummary;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyActiveHoursCalculatorTest {

    private final DailyActiveHoursCalculator calculator = new DailyActiveHoursCalculator();

    @Test
    void treatsWorkerPresenceAsActiveImage() {
        ImageEvidenceSummary summary = new ImageEvidenceSummary(Map.of("worker", 1), List.of(), List.of(), List.of(), List.of(), List.of());

        assertTrue(calculator.isActiveImage(summary));
    }

    @Test
    void treatsMeaningfulChangeFlagsAsActiveImage() {
        ImageEvidenceSummary summary = new ImageEvidenceSummary(Map.of("van", 2), List.of(), List.of(), List.of("more:truck"), List.of(), List.of());

        assertTrue(calculator.isActiveImage(summary));
    }

    @Test
    void treatsOperationalCandidateTagsAsActiveImage() {
        ImageEvidenceSummary summary = new ImageEvidenceSummary(
                Map.of("truck", 1),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(ImageEvidenceCandidateTag.NEW_EQUIPMENT.value())
        );

        assertTrue(calculator.isActiveImage(summary));
    }

    @Test
    void ignoresUnchangedParkedVehicleOnlyImage() {
        ImageEvidenceSummary summary = new ImageEvidenceSummary(
                Map.of("van", 3, "car", 1),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(ImageEvidenceCandidateTag.UPPER_PARKING_ACTIVITY.value())
        );

        assertFalse(calculator.isActiveImage(summary));
    }
}

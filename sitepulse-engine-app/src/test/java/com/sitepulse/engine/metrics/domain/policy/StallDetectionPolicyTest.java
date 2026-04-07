package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StallDetectionPolicyTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);
    private final StallDetectionPolicy policy = new StallDetectionPolicy();

    private DailyMetric metricWithCounts(int day, int people, int vehicles) {
        return DailyMetric.restore(day, 1, LocalDate.now().minusDays(day), people, vehicles, 0.0, NOW);
    }

    @Test
    void stallDetectedAfterThreeConsecutiveLowDays() {
        List<DailyMetric> daily = IntStream.range(0, 5)
                .mapToObj(i -> metricWithCounts(i, 1, 0))
                .toList();
        assertTrue(policy.isStalled(daily));
        assertEquals(5, policy.consecutiveLowActivityDays(daily));
    }

    @Test
    void noStallWithHighActivity() {
        List<DailyMetric> daily = IntStream.range(0, 5)
                .mapToObj(i -> metricWithCounts(i, 5, 3))
                .toList();
        assertFalse(policy.isStalled(daily));
        assertEquals(0, policy.consecutiveLowActivityDays(daily));
    }

    @Test
    void noStallWithOnlyTwoLowDays() {
        List<DailyMetric> daily = List.of(
                metricWithCounts(0, 5, 3),
                metricWithCounts(1, 5, 3),
                metricWithCounts(2, 5, 3),
                metricWithCounts(3, 1, 0),
                metricWithCounts(4, 1, 0)
        );
        assertFalse(policy.isStalled(daily));
    }
}

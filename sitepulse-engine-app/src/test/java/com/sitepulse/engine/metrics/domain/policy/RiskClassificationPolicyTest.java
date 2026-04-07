package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.model.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskClassificationPolicyTest {

    private final RiskClassificationPolicy policy = new RiskClassificationPolicy();

    @Test
    void lowRiskWhenNoRollingAverage() {
        assertEquals(RiskLevel.LOW, policy.classify(50.0, null));
    }

    @Test
    void lowRiskWhenDropBelow20Percent() {
        assertEquals(RiskLevel.LOW, policy.classify(85.0, 100.0));
    }

    @Test
    void mediumRiskWhenDropBetween20And40Percent() {
        assertEquals(RiskLevel.MEDIUM, policy.classify(75.0, 100.0));
    }

    @Test
    void highRiskWhenDropAbove40Percent() {
        assertEquals(RiskLevel.HIGH, policy.classify(50.0, 100.0));
    }

    @Test
    void lowRiskWhenZeroRollingAverage() {
        assertEquals(RiskLevel.LOW, policy.classify(50.0, 0.0));
    }
}

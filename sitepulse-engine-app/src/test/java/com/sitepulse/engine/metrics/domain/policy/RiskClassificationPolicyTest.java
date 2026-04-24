package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.enums.RiskLevel;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskClassificationPolicyTest {

    private final RiskClassificationPolicy policy = new RiskClassificationPolicy();

    @Test
    void lowRiskWhenNoRollingAverage() {
        assertEquals(RiskLevel.LOW, policy.classify(BigDecimal.valueOf(50.0), null));
    }

    @Test
    void lowRiskWhenDropBelow20Percent() {
        assertEquals(RiskLevel.LOW, policy.classify(BigDecimal.valueOf(85.0), BigDecimal.valueOf(100.0)));
    }

    @Test
    void mediumRiskWhenDropBetween20And40Percent() {
        assertEquals(RiskLevel.MEDIUM, policy.classify(BigDecimal.valueOf(75.0), BigDecimal.valueOf(100.0)));
    }

    @Test
    void highRiskWhenDropAbove40Percent() {
        assertEquals(RiskLevel.HIGH, policy.classify(BigDecimal.valueOf(50.0), BigDecimal.valueOf(100.0)));
    }

    @Test
    void lowRiskWhenZeroRollingAverage() {
        assertEquals(RiskLevel.LOW, policy.classify(BigDecimal.valueOf(50.0), BigDecimal.ZERO));
    }
}

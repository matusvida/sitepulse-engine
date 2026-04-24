package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.enums.RiskLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class RiskClassificationPolicy {

    public RiskLevel classify(BigDecimal activityIndex, BigDecimal rollingAverage) {
        if (rollingAverage == null || rollingAverage.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevel.LOW;
        }
        BigDecimal dropPercent = rollingAverage.subtract(activityIndex)
                .multiply(BigDecimal.valueOf(100))
                .divide(rollingAverage, 4, RoundingMode.HALF_UP);
        if (dropPercent.compareTo(BigDecimal.valueOf(40)) > 0) {
            return RiskLevel.HIGH;
        } else if (dropPercent.compareTo(BigDecimal.valueOf(20)) > 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}

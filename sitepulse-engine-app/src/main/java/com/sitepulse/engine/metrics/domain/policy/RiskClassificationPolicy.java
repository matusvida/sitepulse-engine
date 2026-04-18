package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.enums.RiskLevel;

public class RiskClassificationPolicy {

    public RiskLevel classify(double activityIndex, Double rollingAverage) {
        if (rollingAverage == null || rollingAverage <= 0) {
            return RiskLevel.LOW;
        }
        double dropPercent = ((rollingAverage - activityIndex) / rollingAverage) * 100.0;
        if (dropPercent > 40) {
            return RiskLevel.HIGH;
        } else if (dropPercent > 20) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}

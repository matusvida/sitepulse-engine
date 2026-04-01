package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.model.WeeklyMetric;
import java.util.List;

public class DeclineDetectionPolicy {

    private static final int MIN_CONSECUTIVE_WEEKS = 2;

    public long consecutiveNegativeWeeks(List<WeeklyMetric> recentWeekly) {
        return recentWeekly.stream()
                .takeWhile(row -> row.getProgressDelta() != null && row.getProgressDelta() < 0)
                .count();
    }

    public boolean isDeclining(List<WeeklyMetric> recentWeekly) {
        return consecutiveNegativeWeeks(recentWeekly) >= MIN_CONSECUTIVE_WEEKS;
    }
}

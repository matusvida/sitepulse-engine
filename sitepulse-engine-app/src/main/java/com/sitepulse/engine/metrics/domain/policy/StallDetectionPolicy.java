package com.sitepulse.engine.metrics.domain.policy;

import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import java.util.List;

public class StallDetectionPolicy {

    private static final int STALL_THRESHOLD = 2;
    private static final int MIN_CONSECUTIVE_DAYS = 3;

    public long consecutiveLowActivityDays(List<DailyMetric> recentDaily) {
        return recentDaily.reversed().stream()
                .takeWhile(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount())
                        + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()) <= STALL_THRESHOLD)
                .count();
    }

    public boolean isStalled(List<DailyMetric> recentDaily) {
        return consecutiveLowActivityDays(recentDaily) >= MIN_CONSECUTIVE_DAYS;
    }
}

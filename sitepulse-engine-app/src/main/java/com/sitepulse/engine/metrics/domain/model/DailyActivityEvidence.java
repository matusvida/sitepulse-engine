package com.sitepulse.engine.metrics.domain.model;

public record DailyActivityEvidence(
        int imageCount,
        int peopleCount,
        int vehicleCount,
        double activeHours,
        double maxActivityScore,
        double maxChangeScore,
        double averageQualityScore,
        int movementSignalCount,
        boolean loadingActivityDetected,
        boolean equipmentChangeDetected,
        boolean rainObserved,
        boolean snowObserved,
        boolean limitedVisibility
) {
}

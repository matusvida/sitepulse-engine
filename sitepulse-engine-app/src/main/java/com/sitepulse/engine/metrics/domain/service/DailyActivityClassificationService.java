package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.metrics.domain.enums.DailyActivityReasonCode;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.DailyObservationConfidence;
import com.sitepulse.engine.metrics.domain.enums.DailyWeatherStatus;
import com.sitepulse.engine.metrics.domain.model.DailyActivityAssessment;
import com.sitepulse.engine.metrics.domain.model.DailyActivityEvidence;
import java.util.ArrayList;
import java.util.List;

public class DailyActivityClassificationService {

    private static final int MIN_IMAGES_FOR_CONFIDENT_DAY = 2;
    private static final double MIN_QUALITY_FOR_CONFIDENT_DAY = 2.0;
    private static final double HIGH_QUALITY_THRESHOLD = 3.5;
    private static final double CHANGE_SCORE_ACTIVITY_THRESHOLD = 2.5;
    private static final double ACTIVITY_SCORE_THRESHOLD = 8.0;

    public DailyActivityAssessment classify(DailyActivityEvidence evidence) {
        List<DailyActivityReasonCode> reasons = new ArrayList<>();
        DailyWeatherStatus weatherStatus = resolveWeatherStatus(evidence, reasons);

        boolean enoughImages = evidence.imageCount() >= MIN_IMAGES_FOR_CONFIDENT_DAY;
        boolean goodVisibility = evidence.averageQualityScore() >= MIN_QUALITY_FOR_CONFIDENT_DAY;
        boolean strongMovementSignal = evidence.activeHours() >= 1.0
                || evidence.movementSignalCount() >= 2
                || evidence.maxChangeScore() >= CHANGE_SCORE_ACTIVITY_THRESHOLD
                || evidence.loadingActivityDetected()
                || evidence.equipmentChangeDetected()
                || evidence.maxActivityScore() >= ACTIVITY_SCORE_THRESHOLD;

        if (evidence.activeHours() >= 1.0) {
            reasons.add(DailyActivityReasonCode.ACTIVE_HOURS_RECORDED);
        }
        if (evidence.movementSignalCount() >= 2) {
            reasons.add(DailyActivityReasonCode.MOVEMENT_SIGNALS_PRESENT);
        }
        if (evidence.peopleCount() > 0) {
            reasons.add(DailyActivityReasonCode.WORKER_ACTIVITY_PRESENT);
        }
        if (evidence.vehicleCount() > 0) {
            reasons.add(DailyActivityReasonCode.VEHICLE_ACTIVITY_PRESENT);
        }
        if (evidence.maxChangeScore() >= CHANGE_SCORE_ACTIVITY_THRESHOLD) {
            reasons.add(DailyActivityReasonCode.CHANGE_SCORE_ELEVATED);
        }
        if (evidence.loadingActivityDetected()) {
            reasons.add(DailyActivityReasonCode.LOADING_ACTIVITY_DETECTED);
        }
        if (evidence.equipmentChangeDetected()) {
            reasons.add(DailyActivityReasonCode.EQUIPMENT_CHANGE_DETECTED);
        }
        if (!enoughImages) {
            reasons.add(DailyActivityReasonCode.LOW_IMAGE_COVERAGE);
        }
        if (!goodVisibility || evidence.limitedVisibility()) {
            reasons.add(DailyActivityReasonCode.LIMITED_VISIBILITY);
        }

        DailyActivityStatus activityStatus;
        DailyObservationConfidence confidence;
        if (!enoughImages || !goodVisibility) {
            activityStatus = DailyActivityStatus.UNKNOWN;
            confidence = DailyObservationConfidence.LOW;
        } else if (strongMovementSignal) {
            activityStatus = DailyActivityStatus.ACTIVE;
            confidence = evidence.imageCount() >= 4 && evidence.averageQualityScore() >= HIGH_QUALITY_THRESHOLD
                    ? DailyObservationConfidence.HIGH
                    : DailyObservationConfidence.MEDIUM;
        } else {
            activityStatus = DailyActivityStatus.INACTIVE;
            confidence = evidence.imageCount() >= 4 && evidence.averageQualityScore() >= HIGH_QUALITY_THRESHOLD
                    ? DailyObservationConfidence.HIGH
                    : DailyObservationConfidence.MEDIUM;
            reasons.add(DailyActivityReasonCode.NO_MOVEMENT_SIGNAL);
        }

        boolean weatherImpacted = weatherStatus != DailyWeatherStatus.CLEAR_OR_NORMAL
                && weatherStatus != DailyWeatherStatus.UNCLEAR
                && (activityStatus != DailyActivityStatus.ACTIVE || evidence.activeHours() < 1.0);
        if (weatherImpacted) {
            reasons.add(DailyActivityReasonCode.WEATHER_IMPACT_LIKELY);
        }

        return new DailyActivityAssessment(
                activityStatus,
                confidence,
                weatherStatus,
                weatherImpacted,
                List.copyOf(reasons),
                buildSummary(activityStatus, weatherStatus, weatherImpacted, reasons)
        );
    }

    private DailyWeatherStatus resolveWeatherStatus(DailyActivityEvidence evidence, List<DailyActivityReasonCode> reasons) {
        if (evidence.snowObserved()) {
            reasons.add(DailyActivityReasonCode.SNOW_OBSERVED);
            return DailyWeatherStatus.SNOW;
        }
        if (evidence.rainObserved()) {
            reasons.add(DailyActivityReasonCode.RAIN_OBSERVED);
            return DailyWeatherStatus.RAIN;
        }
        if (evidence.imageCount() == 0) {
            return DailyWeatherStatus.UNCLEAR;
        }
        return DailyWeatherStatus.CLEAR_OR_NORMAL;
    }

    private String buildSummary(
            DailyActivityStatus activityStatus,
            DailyWeatherStatus weatherStatus,
            boolean weatherImpacted,
            List<DailyActivityReasonCode> reasons
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("activity=").append(activityStatus.toPersistenceValue());
        builder.append(", weather=").append(weatherStatus.toPersistenceValue());
        if (weatherImpacted) {
            builder.append(", weather_impact=likely");
        }
        if (!reasons.isEmpty()) {
            builder.append(", reasons=")
                    .append(reasons.stream().map(DailyActivityReasonCode::toPersistenceValue).distinct().toList());
        }
        return builder.toString();
    }
}

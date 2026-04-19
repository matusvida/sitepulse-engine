package com.sitepulse.engine.metrics.domain.enums;

public enum DailyActivityReasonCode {
    ACTIVE_HOURS_RECORDED("active_hours_recorded"),
    MOVEMENT_SIGNALS_PRESENT("movement_signals_present"),
    WORKER_ACTIVITY_PRESENT("worker_activity_present"),
    VEHICLE_ACTIVITY_PRESENT("vehicle_activity_present"),
    CHANGE_SCORE_ELEVATED("change_score_elevated"),
    LOADING_ACTIVITY_DETECTED("loading_activity_detected"),
    EQUIPMENT_CHANGE_DETECTED("equipment_change_detected"),
    NO_MOVEMENT_SIGNAL("no_movement_signal"),
    LOW_IMAGE_COVERAGE("low_image_coverage"),
    LIMITED_VISIBILITY("limited_visibility"),
    RAIN_OBSERVED("rain_observed"),
    SNOW_OBSERVED("snow_observed"),
    WEATHER_IMPACT_LIKELY("weather_impact_likely");

    private final String persistenceValue;

    DailyActivityReasonCode(String persistenceValue) {
        this.persistenceValue = persistenceValue;
    }

    public String toPersistenceValue() {
        return persistenceValue;
    }
}

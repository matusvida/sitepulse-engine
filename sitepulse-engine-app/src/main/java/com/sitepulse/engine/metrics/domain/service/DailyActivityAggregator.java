package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.detection.domain.enums.DetectionClassGroup;
import com.sitepulse.engine.metrics.domain.model.DetectionActivitySample;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyActivityAggregator {

    private static final int MIN_DETECTIONS_ACTIVE_HOUR = 3;

    public record DailyAggregation(int peopleCount, int vehicleCount, double activeHours) {}

    public DailyAggregation aggregate(List<DetectionActivitySample> samples) {
        Map<Integer, Integer> peoplePerImage = new HashMap<>();
        Map<Integer, Integer> vehiclePerImage = new HashMap<>();
        Map<Integer, Integer> hoursWithDetections = new HashMap<>();

        for (DetectionActivitySample row : samples) {
            DetectionClassGroup classGroup = DetectionClassGroup.fromPersistenceValue(row.classGroup());
            if (classGroup == DetectionClassGroup.PEOPLE) {
                peoplePerImage.merge(row.imageId(), 1, Integer::sum);
            } else if (isVehicle(classGroup)) {
                vehiclePerImage.merge(row.imageId(), 1, Integer::sum);
            }
            hoursWithDetections.merge(row.capturedAt().getHour(), 1, Integer::sum);
        }

        int peopleCount = peoplePerImage.values().stream().max(Integer::compareTo).orElse(0);
        int vehicleCount = vehiclePerImage.values().stream().max(Integer::compareTo).orElse(0);
        double activeHours = hoursWithDetections.values().stream().filter(count -> count >= MIN_DETECTIONS_ACTIVE_HOUR).count();

        return new DailyAggregation(peopleCount, vehicleCount, activeHours);
    }

    private boolean isVehicle(DetectionClassGroup classGroup) {
        return switch (classGroup) {
            case LIGHT_VEHICLE, TRUCK, TRANSPORT, OTHER_VEHICLE -> true;
            default -> false;
        };
    }
}

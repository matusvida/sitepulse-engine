package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.metrics.domain.model.DetectionActivitySample;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyActivityAggregator {

    private static final List<String> VEHICLE_CLASSES = List.of("car", "truck", "bus");
    private static final List<String> PERSON_CLASSES = List.of("person");
    private static final int MIN_DETECTIONS_ACTIVE_HOUR = 3;

    public record DailyAggregation(int peopleCount, int vehicleCount, double activeHours) {}

    public DailyAggregation aggregate(List<DetectionActivitySample> samples) {
        Map<Integer, Integer> peoplePerImage = new HashMap<>();
        Map<Integer, Integer> vehiclePerImage = new HashMap<>();
        Map<Integer, Integer> hoursWithDetections = new HashMap<>();

        for (DetectionActivitySample row : samples) {
            if (PERSON_CLASSES.contains(row.className())) {
                peoplePerImage.merge(row.imageId(), 1, Integer::sum);
            } else if (VEHICLE_CLASSES.contains(row.className())) {
                vehiclePerImage.merge(row.imageId(), 1, Integer::sum);
            }
            hoursWithDetections.merge(row.capturedAt().getHour(), 1, Integer::sum);
        }

        int peopleCount = peoplePerImage.values().stream().max(Integer::compareTo).orElse(0);
        int vehicleCount = vehiclePerImage.values().stream().max(Integer::compareTo).orElse(0);
        double activeHours = hoursWithDetections.values().stream().filter(count -> count >= MIN_DETECTIONS_ACTIVE_HOUR).count();

        return new DailyAggregation(peopleCount, vehicleCount, activeHours);
    }
}

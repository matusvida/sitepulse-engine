package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.application.enums.ImageEvidenceCandidateTag;
import com.sitepulse.engine.detection.application.enums.ImageEvidenceChangeFlag;
import com.sitepulse.engine.detection.application.enums.ImageEvidenceFirstAppearanceFlag;
import com.sitepulse.engine.detection.application.enums.ImageEvidenceSummaryField;
import com.sitepulse.engine.detection.domain.enums.DetectionClassGroup;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionClassDefinition;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.port.DetectionClassCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ImageEvidenceScoringService {

    private final JsonUtils jsonUtils;
    private final DetectionClassCatalog detectionClassCatalog;

    public ImageEvidenceScoringService(JsonUtils jsonUtils, DetectionClassCatalog detectionClassCatalog) {
        this.jsonUtils = jsonUtils;
        this.detectionClassCatalog = detectionClassCatalog;
    }

    public ImageEvidenceFeatures score(
            DetectionImage image,
            DetectionOutcome outcome,
            List<DetectedObject> detections,
            DetectionImage previousImage,
            List<DetectedObject> previousDetections
    ) {
        Map<String, Long> classCounts = countByClass(detections);
        Map<String, Long> previousClassCounts = countByClass(previousDetections);
        Map<DetectionClassGroup, Long> groupCounts = countByGroup(detections);
        List<String> qualityFlags = normalizeWarnings(outcome.warnings());
        List<String> firstAppearanceFlags = firstAppearanceFlags(detections, previousClassCounts);
        List<String> changeFlags = changeFlags(classCounts, previousClassCounts);
        List<String> candidateTags = candidateTags(groupCounts, firstAppearanceFlags, changeFlags, qualityFlags);

        double activityScore = round(activityScore(detections));
        double changeScore = round(changeScore(classCounts, previousClassCounts, firstAppearanceFlags, image, previousImage));
        double qualityScore = round(qualityScore(qualityFlags, outcome.skipped()));
        double overallScore = round(activityScore * 0.45 + changeScore * 0.35 + qualityScore * 0.20);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(ImageEvidenceSummaryField.CLASS_COUNTS.key(), classCounts);
        summary.put(ImageEvidenceSummaryField.DOMINANT_CLASSES.key(), dominantClasses(classCounts));
        summary.put(ImageEvidenceSummaryField.FIRST_APPEARANCE_FLAGS.key(), firstAppearanceFlags);
        summary.put(ImageEvidenceSummaryField.CHANGE_FLAGS.key(), changeFlags);
        summary.put(ImageEvidenceSummaryField.QUALITY_FLAGS.key(), qualityFlags);
        summary.put(ImageEvidenceSummaryField.CANDIDATE_TAGS.key(), candidateTags);

        return new ImageEvidenceFeatures(
                normalizeWeather(outcome.weatherNote()),
                activityScore,
                changeScore,
                qualityScore,
                overallScore,
                jsonUtils.write(summary)
        );
    }

    private Map<String, Long> countByClass(List<DetectedObject> detections) {
        return detections == null ? Map.of() : detections.stream()
                .map(this::resolvedClassName)
                .filter(className -> className != null && !className.isBlank())
                .collect(Collectors.groupingBy(
                        className -> className.toLowerCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<DetectionClassGroup, Long> countByGroup(List<DetectedObject> detections) {
        return detections == null ? Map.of() : detections.stream()
                .map(this::resolveClassGroup)
                .collect(Collectors.groupingBy(group -> group, LinkedHashMap::new, Collectors.counting()));
    }

    private List<String> dominantClasses(Map<String, Long> classCounts) {
        return classCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> firstAppearanceFlags(List<DetectedObject> detections, Map<String, Long> previousClassCounts) {
        return (detections == null ? List.<DetectedObject>of() : detections).stream()
                .map(this::resolvedClassName)
                .filter(className -> className != null && !className.isBlank())
                .distinct()
                .filter(this::noteworthyForFirstAppearance)
                .filter(className -> previousClassCounts.getOrDefault(className, 0L) == 0L)
                .map(ImageEvidenceFirstAppearanceFlag.FIRST::format)
                .toList();
    }

    private List<String> changeFlags(Map<String, Long> classCounts, Map<String, Long> previousClassCounts) {
        List<String> flags = new ArrayList<>();
        for (String className : unionKeys(classCounts, previousClassCounts)) {
            long current = classCounts.getOrDefault(className, 0L);
            long previous = previousClassCounts.getOrDefault(className, 0L);
            if (current > previous) {
                flags.add(ImageEvidenceChangeFlag.MORE.format(className));
            } else if (current < previous) {
                flags.add(ImageEvidenceChangeFlag.LESS.format(className));
            }
        }
        return flags;
    }

    private List<String> candidateTags(
            Map<DetectionClassGroup, Long> groupCounts,
            List<String> firstAppearanceFlags,
            List<String> changeFlags,
            List<String> qualityFlags
    ) {
        List<String> tags = new ArrayList<>();
        long vehicleCount = onSiteVehicleCount(groupCounts);
        if (vehicleCount >= 3) {
            tags.add(ImageEvidenceCandidateTag.UPPER_PARKING_ACTIVITY.value());
        }
        if (groupCounts.getOrDefault(DetectionClassGroup.EARTHMOVING, 0L) > 0
                && truckRelatedCount(groupCounts) > 0) {
            tags.add(ImageEvidenceCandidateTag.LOADING_ACTIVITY.value());
        }
        if (activityScore(groupCounts) >= 8.0) {
            tags.add(ImageEvidenceCandidateTag.PEAK_ACTIVITY.value());
        }
        if (!firstAppearanceFlags.isEmpty()) {
            tags.add(ImageEvidenceCandidateTag.NEW_EQUIPMENT.value());
        }
        if (!changeFlags.isEmpty()) {
            tags.add(ImageEvidenceCandidateTag.EQUIPMENT_CHANGE.value());
        }
        if (!qualityFlags.isEmpty()) {
            tags.add(ImageEvidenceCandidateTag.QUALITY_LIMITED.value());
        }
        return tags;
    }

    private List<String> normalizeWarnings(List<String> warnings) {
        return warnings == null ? List.of() : warnings.stream()
                .filter(war -> war != null && !war.isBlank())
                .map(war -> war.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    private double activityScore(List<DetectedObject> detections) {
        double score = 0.0;
        Map<DetectionClassGroup, Long> groupCounts = countByGroup(detections);
        for (Map.Entry<DetectionClassGroup, Long> entry : groupCounts.entrySet()) {
            score += entry.getValue() * weight(entry.getKey());
        }
        if (groupCounts.getOrDefault(DetectionClassGroup.EARTHMOVING, 0L) > 0
                && truckRelatedCount(groupCounts) > 0) {
            score += 2.0;
        }
        return score;
    }

    private double activityScore(Map<DetectionClassGroup, Long> groupCounts) {
        double score = 0.0;
        for (Map.Entry<DetectionClassGroup, Long> entry : groupCounts.entrySet()) {
            score += entry.getValue() * weight(entry.getKey());
        }
        if (groupCounts.getOrDefault(DetectionClassGroup.EARTHMOVING, 0L) > 0
                && truckRelatedCount(groupCounts) > 0) {
            score += 2.0;
        }
        return score;
    }

    private double changeScore(
            Map<String, Long> classCounts,
            Map<String, Long> previousClassCounts,
            List<String> firstAppearanceFlags,
            DetectionImage image,
            DetectionImage previousImage
    ) {
        double delta = 0.0;
        for (String key : unionKeys(classCounts, previousClassCounts)) {
            delta += Math.abs(classCounts.getOrDefault(key, 0L) - previousClassCounts.getOrDefault(key, 0L));
        }
        if (!firstAppearanceFlags.isEmpty()) {
            delta += firstAppearanceFlags.size() * 1.5;
        }
        if (previousImage == null) {
            delta += 1.0;
        }
        if (image.getCapturedAt() != null && previousImage != null && previousImage.getCapturedAt() != null) {
            long minutes = Math.abs(java.time.Duration.between(previousImage.getCapturedAt(), image.getCapturedAt()).toMinutes());
            if (minutes >= 60) {
                delta += 1.0;
            }
        }
        return delta;
    }

    private double qualityScore(List<String> qualityFlags, boolean skipped) {
        double score = skipped ? 1.0 : 5.0;
        score -= qualityFlags.size() * 1.5;
        return Math.clamp(score, 0.0, 5.0);
    }

    private long onSiteVehicleCount(Map<DetectionClassGroup, Long> groupCounts) {
        return groupCounts.getOrDefault(DetectionClassGroup.LIGHT_VEHICLE, 0L)
                + groupCounts.getOrDefault(DetectionClassGroup.TRUCK, 0L)
                + groupCounts.getOrDefault(DetectionClassGroup.TRANSPORT, 0L)
                + groupCounts.getOrDefault(DetectionClassGroup.OTHER_VEHICLE, 0L);
    }

    private long truckRelatedCount(Map<DetectionClassGroup, Long> groupCounts) {
        return groupCounts.getOrDefault(DetectionClassGroup.TRUCK, 0L)
                + groupCounts.getOrDefault(DetectionClassGroup.OTHER_VEHICLE, 0L);
    }

    private boolean noteworthyForFirstAppearance(String className) {
        DetectionClassGroup group = resolveClassGroup(className, null);
        return switch (group) {
            case EARTHMOVING, LIFTING, PAVING, POWER, STRUCTURE, AERIAL, TRUCK, OTHER_EQUIPMENT -> true;
            default -> false;
        };
    }

    private double weight(DetectionClassGroup group) {
        return switch (group) {
            case EARTHMOVING -> 4.0;
            case LIFTING, PAVING -> 3.5;
            case TRUCK -> 3.0;
            case PEOPLE -> 2.0;
            case LIGHT_VEHICLE, TRANSPORT, OTHER_VEHICLE -> 1.5;
            case STRUCTURE, POWER, AERIAL, OTHER_EQUIPMENT -> 2.5;
            case UNKNOWN -> 1.0;
        };
    }

    private String normalizeWeather(String weatherNote) {
        if (weatherNote == null || weatherNote.isBlank()) {
            return "unclear";
        }
        String normalized = weatherNote.trim().toLowerCase(Locale.ROOT);
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private List<String> unionKeys(Map<String, Long> left, Map<String, Long> right) {
        return java.util.stream.Stream.concat(left.keySet().stream(), right.keySet().stream())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private String resolvedClassName(DetectedObject detection) {
        return resolveDetectionClass(detection)
                .map(DetectionClassDefinition::className)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .orElseGet(() -> detection.className() == null ? null : detection.className().toLowerCase(Locale.ROOT));
    }

    private DetectionClassGroup resolveClassGroup(DetectedObject detection) {
        return resolveDetectionClass(detection)
                .map(DetectionClassDefinition::classGroup)
                .map(DetectionClassGroup::fromPersistenceValue)
                .orElseGet(() -> resolveClassGroup(detection.className(), detection.classId()));
    }

    private DetectionClassGroup resolveClassGroup(String className, Integer classId) {
        if (classId != null) {
            Optional<DetectionClassDefinition> byId = detectionClassCatalog.findById(classId);
            if (byId.isPresent()) {
                return DetectionClassGroup.fromPersistenceValue(byId.get().classGroup());
            }
        }
        if (className != null && !className.isBlank()) {
            return detectionClassCatalog.findByName(className)
                    .map(DetectionClassDefinition::classGroup)
                    .map(DetectionClassGroup::fromPersistenceValue)
                    .orElse(DetectionClassGroup.UNKNOWN);
        }
        return DetectionClassGroup.UNKNOWN;
    }

    private Optional<DetectionClassDefinition> resolveDetectionClass(DetectedObject detection) {
        if (detection.classId() != null) {
            Optional<DetectionClassDefinition> byId = detectionClassCatalog.findById(detection.classId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (detection.className() != null && !detection.className().isBlank()) {
            return detectionClassCatalog.findByName(detection.className());
        }
        return Optional.empty();
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}

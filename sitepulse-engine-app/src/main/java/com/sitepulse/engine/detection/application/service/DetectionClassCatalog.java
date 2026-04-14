package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassRepository;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionClassCatalog {

    private static final List<String> CLASS_GROUP_ORDER = List.of(
            "people",
            "light_vehicle",
            "truck",
            "transport",
            "earthmoving",
            "lifting",
            "paving",
            "structure",
            "power",
            "aerial",
            "other_vehicle",
            "other_equipment",
            "unknown"
    );

    private final DetectionClassRepository detectionClassRepository;
    private final AtomicReference<Map<Integer, DetectionClassEntity>> byIdCache = new AtomicReference<>();
    private final AtomicReference<Map<String, DetectionClassEntity>> byNameCache = new AtomicReference<>();
    private final AtomicReference<Map<String, List<DetectionClassEntity>>> byGroupCache = new AtomicReference<>();

    public Map<Integer, DetectionClassEntity> byId() {
        ensureLoaded();
        return byIdCache.get();
    }

    public Optional<DetectionClassEntity> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        ensureLoaded();
        return Optional.ofNullable(byIdCache.get().get(id));
    }

    public Optional<DetectionClassEntity> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        ensureLoaded();
        return Optional.ofNullable(byNameCache.get().get(normalize(name)));
    }

    public Map<String, List<DetectionClassEntity>> byGroup() {
        ensureLoaded();
        return byGroupCache.get();
    }

    public DetectionClassEntity resolveByNameOrDefault(String name, String defaultName) {
        return findByName(name)
                .or(() -> findByName(defaultName))
                .orElseThrow(() -> new IllegalStateException("Detection classes missing required default: " + defaultName));
    }

    public DetectionClassEntity resolveBestEffort(String label) {
        String normalized = normalize(label);
        return findByName(normalized)
                .or(() -> alias(normalized))
                .or(() -> findByName("other_equipment"))
                .orElseThrow(() -> new IllegalStateException("Detection classes missing required fallback taxonomy"));
    }

    public Integer resolveIdBestEffort(String label) {
        return resolveBestEffort(label).getId();
    }

    public void refresh() {
        byIdCache.set(null);
        byNameCache.set(null);
        byGroupCache.set(null);
    }

    private void ensureLoaded() {
        if (byIdCache.get() != null && byNameCache.get() != null) {
            return;
        }
        synchronized (this) {
            if (byIdCache.get() != null && byNameCache.get() != null) {
                return;
            }
            Map<Integer, DetectionClassEntity> byId = new HashMap<>();
            Map<String, DetectionClassEntity> byName = new HashMap<>();
            Map<String, List<DetectionClassEntity>> byGroup = new LinkedHashMap<>();
            for (DetectionClassEntity entity : detectionClassRepository.findAll().stream()
                    .sorted(Comparator.comparing(DetectionClassEntity::getId))
                    .toList()) {
                byId.put(entity.getId(), entity);
                byName.put(normalize(entity.getClassName()), entity);
                String group = normalizeGroup(entity.getClassGroup());
                byGroup.computeIfAbsent(group, ignored -> new ArrayList<>()).add(entity);
            }
            byIdCache.set(Map.copyOf(byId));
            byNameCache.set(Map.copyOf(byName));
            byGroupCache.set(orderGroups(byGroup));
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGroup(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return normalize(value);
    }

    private Map<String, List<DetectionClassEntity>> orderGroups(Map<String, List<DetectionClassEntity>> grouped) {
        Map<String, List<DetectionClassEntity>> ordered = new LinkedHashMap<>();
        for (String group : CLASS_GROUP_ORDER) {
            List<DetectionClassEntity> entries = grouped.get(group);
            if (entries != null && !entries.isEmpty()) {
                ordered.put(group, List.copyOf(entries));
            }
        }
        grouped.forEach((group, entries) -> {
            if (!ordered.containsKey(group)) {
                ordered.put(group, List.copyOf(entries));
            }
        });
        return Collections.unmodifiableMap(ordered);
    }

    private Optional<DetectionClassEntity> alias(String normalized) {
        return switch (normalized) {
            case "man", "woman", "worker", "operator", "supervisor", "human" -> findByName("person");
            case "persons", "people" -> findByName("person");
            case "motorbike", "motorbike.", "motorcycle", "motor_cycle" -> findByName("motorcycle");
            case "bike", "bicycle" -> findByName("bicycle");
            case "pickup", "pickuptruck", "pickup_truck", "ute" -> findByName("pickup_truck");
            case "lorry", "delivery_truck" -> findByName("truck");
            case "dumptruck", "dump_truck" -> findByName("dump_truck");
            case "mixer", "cement_mixer", "concrete_mixer", "concrete_mixer_truck" -> findByName("concrete_mixer_truck");
            case "excavator", "digger" -> findByName("excavator");
            case "backhoe" -> findByName("backhoe_loader");
            case "loader", "wheel_loader" -> findByName("wheel_loader");
            case "skidsteer", "skid_steer", "bobcat" -> findByName("skid_steer_loader");
            case "bulldozer", "dozer" -> findByName("bulldozer");
            case "grader" -> findByName("grader");
            case "roller", "compactor" -> findByName("roller");
            case "forklift" -> findByName("forklift");
            case "telehandler" -> findByName("telehandler");
            case "paver" -> findByName("paver");
            case "crane", "mobile_crane" -> findByName("crane_mobile");
            case "tower_crane" -> findByName("crane_tower");
            case "truck_crane", "crane_truck" -> findByName("crane_truck");
            case "hoist" -> findByName("hoist");
            case "cherrypicker", "cherry_picker", "boom_lift" -> findByName("cherry_picker");
            case "scaffold", "scaffolding" -> findByName("scaffolding");
            case "generator" -> findByName("generator");
            case "helicopter" -> findByName("helicopter");
            case "car", "sedan", "suv", "hatchback" -> findByName("car");
            case "van" -> findByName("van");
            case "bus" -> findByName("bus");
            default -> heuristicFallback(normalized);
        };
    }

    private Optional<DetectionClassEntity> heuristicFallback(String normalized) {
        if (normalized.contains("person") || normalized.contains("worker") || normalized.contains("human")) {
            return findByName("person");
        }
        if (normalized.contains("truck") || normalized.contains("car") || normalized.contains("van") || normalized.contains("bus") || normalized.contains("vehicle")) {
            return findByName("other_vehicle").or(() -> findByName("car"));
        }
        if (normalized.contains("crane") || normalized.contains("excav") || normalized.contains("loader") || normalized.contains("forklift") || normalized.contains("bulldozer") || normalized.contains("grader") || normalized.contains("roller") || normalized.contains("paver") || normalized.contains("hoist") || normalized.contains("generator")) {
            return findByName("other_equipment");
        }
        return findByName("other_equipment");
    }
}

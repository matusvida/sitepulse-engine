package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassRepository;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionClassCatalog {

    private final DetectionClassRepository detectionClassRepository;
    private final AtomicReference<Map<Integer, DetectionClassEntity>> byIdCache = new AtomicReference<>();
    private final AtomicReference<Map<String, DetectionClassEntity>> byNameCache = new AtomicReference<>();

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
            for (DetectionClassEntity entity : detectionClassRepository.findAll()) {
                byId.put(entity.getId(), entity);
                byName.put(normalize(entity.getClassName()), entity);
            }
            byIdCache.set(Map.copyOf(byId));
            byNameCache.set(Map.copyOf(byName));
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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

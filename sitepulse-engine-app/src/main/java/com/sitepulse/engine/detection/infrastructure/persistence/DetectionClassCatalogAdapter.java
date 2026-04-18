package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.enums.DetectionClassGroup;
import com.sitepulse.engine.detection.domain.model.DetectionClassDefinition;
import com.sitepulse.engine.detection.domain.model.DetectionTaxonomy;
import com.sitepulse.engine.detection.domain.port.DetectionClassCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
public class DetectionClassCatalogAdapter implements DetectionClassCatalog {

    private final DetectionClassRepository detectionClassRepository;
    private final AtomicReference<Map<Integer, DetectionClassDefinition>> byIdCache = new AtomicReference<>();
    private final AtomicReference<Map<String, DetectionClassDefinition>> byNameCache = new AtomicReference<>();
    private final AtomicReference<Map<String, List<DetectionClassDefinition>>> byGroupCache = new AtomicReference<>();

    @Override
    public Map<Integer, DetectionClassDefinition> byId() {
        ensureLoaded();
        return byIdCache.get();
    }

    @Override
    public Optional<DetectionClassDefinition> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        ensureLoaded();
        return Optional.ofNullable(byIdCache.get().get(id));
    }

    @Override
    public Optional<DetectionClassDefinition> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        ensureLoaded();
        return Optional.ofNullable(byNameCache.get().get(normalize(name)));
    }

    @Override
    public Map<String, List<DetectionClassDefinition>> byGroup() {
        ensureLoaded();
        return byGroupCache.get();
    }

    @Override
    public DetectionClassDefinition resolveByNameOrDefault(String name, String defaultName) {
        return findByName(name)
                .or(() -> findByName(defaultName))
                .orElseThrow(() -> new IllegalStateException("Detection classes missing required default: " + defaultName));
    }

    @Override
    public DetectionClassDefinition resolveBestEffort(String label) {
        String normalized = normalize(label);
        return findByName(normalized)
                .or(() -> alias(normalized))
                .or(() -> findByName(DetectionTaxonomy.OTHER_EQUIPMENT))
                .orElseThrow(() -> new IllegalStateException("Detection classes missing required fallback taxonomy"));
    }

    @Override
    public Integer resolveIdBestEffort(String label) {
        return resolveBestEffort(label).id();
    }

    @Override
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
            Map<Integer, DetectionClassDefinition> byId = new HashMap<>();
            Map<String, DetectionClassDefinition> byName = new HashMap<>();
            Map<String, List<DetectionClassDefinition>> byGroup = new LinkedHashMap<>();
            for (DetectionClassEntity entity : detectionClassRepository.findAll().stream()
                    .sorted(Comparator.comparing(DetectionClassEntity::getId))
                    .toList()) {
                DetectionClassDefinition definition = toDefinition(entity);
                byId.put(definition.id(), definition);
                byName.put(normalize(definition.className()), definition);
                String group = normalizeGroup(definition.classGroup());
                byGroup.computeIfAbsent(group, ignored -> new ArrayList<>()).add(definition);
            }
            byIdCache.set(Map.copyOf(byId));
            byNameCache.set(Map.copyOf(byName));
            byGroupCache.set(orderGroups(byGroup));
        }
    }

    private DetectionClassDefinition toDefinition(DetectionClassEntity entity) {
        return new DetectionClassDefinition(entity.getId(), entity.getClassName(), entity.getClassGroup());
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGroup(String value) {
        if (value == null || value.isBlank()) {
            return DetectionClassGroup.UNKNOWN.toPersistenceValue();
        }
        return normalize(value);
    }

    private Map<String, List<DetectionClassDefinition>> orderGroups(Map<String, List<DetectionClassDefinition>> grouped) {
        Map<String, List<DetectionClassDefinition>> ordered = new LinkedHashMap<>();
        for (String group : DetectionTaxonomy.CLASS_GROUP_ORDER) {
            List<DetectionClassDefinition> entries = grouped.get(group);
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

    private Optional<DetectionClassDefinition> alias(String normalized) {
        String canonical = DetectionTaxonomy.ALIAS_TO_CANONICAL.get(normalized);
        return canonical == null ? heuristicFallback(normalized) : findByName(canonical);
    }

    private Optional<DetectionClassDefinition> heuristicFallback(String normalized) {
        if (containsAnyKeyword(normalized, DetectionTaxonomy.PERSON_HINT_KEYWORDS)) {
            return findByName(DetectionTaxonomy.PERSON);
        }
        if (containsAnyKeyword(normalized, DetectionTaxonomy.VEHICLE_HINT_KEYWORDS)) {
            return findByName(DetectionTaxonomy.OTHER_VEHICLE).or(() -> findByName(DetectionTaxonomy.CAR));
        }
        if (containsAnyKeyword(normalized, DetectionTaxonomy.EQUIPMENT_HINT_KEYWORDS)) {
            return findByName(DetectionTaxonomy.OTHER_EQUIPMENT);
        }
        return findByName(DetectionTaxonomy.OTHER_EQUIPMENT);
    }

    private boolean containsAnyKeyword(String normalized, List<String> keywords) {
        return keywords.stream().anyMatch(normalized::contains);
    }
}

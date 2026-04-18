package com.sitepulse.engine.detection.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectionClassDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DetectionClassCatalog {

    Map<Integer, DetectionClassDefinition> byId();

    Optional<DetectionClassDefinition> findById(Integer id);

    Optional<DetectionClassDefinition> findByName(String name);

    Map<String, List<DetectionClassDefinition>> byGroup();

    DetectionClassDefinition resolveByNameOrDefault(String name, String defaultName);

    DetectionClassDefinition resolveBestEffort(String label);

    Integer resolveIdBestEffort(String label);

    void refresh();
}

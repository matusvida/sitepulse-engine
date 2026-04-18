package com.sitepulse.engine.alert.domain.port;

import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.enums.AlertStatus;
import java.util.List;
import java.util.Optional;

public interface AlertCatalogRepository {

    List<Alert> findByProject(Integer projectId);

    Optional<Alert> findByIdAndProject(Integer alertId, Integer projectId);

    boolean existsByProjectAndTypeAndStatus(Integer projectId, String type, AlertStatus status);

    List<Alert> findByProjectAndTypeAndStatus(Integer projectId, String type, AlertStatus status);

    Alert save(Alert alert);

    List<Alert> saveAll(List<Alert> alerts);
}

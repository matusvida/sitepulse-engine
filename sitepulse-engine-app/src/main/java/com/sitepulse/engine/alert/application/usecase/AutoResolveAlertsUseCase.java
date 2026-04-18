package com.sitepulse.engine.alert.application.usecase;

import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.enums.AlertStatus;
import com.sitepulse.engine.alert.domain.port.AlertCatalogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoResolveAlertsUseCase {

    private final AlertCatalogRepository alertCatalogRepository;

    @Transactional
    public int resolve(Integer projectId, String type) {
        List<Alert> alerts = alertCatalogRepository.findByProjectAndTypeAndStatus(projectId, type, AlertStatus.OPEN);
        OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        alerts.forEach(alert -> alert.resolve(updatedAt));
        alertCatalogRepository.saveAll(alerts);
        return alerts.size();
    }
}

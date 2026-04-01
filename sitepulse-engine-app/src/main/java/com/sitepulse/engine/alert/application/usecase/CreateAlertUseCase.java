package com.sitepulse.engine.alert.application.usecase;

import com.sitepulse.engine.alert.application.command.CreateAlertCommand;
import com.sitepulse.engine.alert.domain.model.Alert;
import com.sitepulse.engine.alert.domain.model.AlertStatus;
import com.sitepulse.engine.alert.domain.port.AlertCatalogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAlertUseCase {

    private final AlertCatalogRepository alertCatalogRepository;

    @Transactional
    public void create(CreateAlertCommand command) {
        if (alertCatalogRepository.existsByProjectAndTypeAndStatus(command.projectId(), command.type(), AlertStatus.OPEN)) {
            return;
        }
        alertCatalogRepository.save(Alert.create(
                command.projectId(),
                command.type(),
                command.severity(),
                command.summary(),
                command.details(),
                command.recommendedActions(),
                OffsetDateTime.now(ZoneOffset.UTC)
        ));
    }
}

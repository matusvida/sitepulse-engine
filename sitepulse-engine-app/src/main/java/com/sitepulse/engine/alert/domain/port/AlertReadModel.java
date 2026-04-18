package com.sitepulse.engine.alert.domain.port;

import com.sitepulse.engine.alert.domain.model.Alert;
import java.util.List;

public interface AlertReadModel {

    List<Alert> findByProjectFiltered(Integer projectId, String type, String severity, String status);
}

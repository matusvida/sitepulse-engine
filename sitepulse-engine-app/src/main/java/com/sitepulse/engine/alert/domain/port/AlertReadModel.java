package com.sitepulse.engine.alert.domain.port;

import com.sitepulse.engine.alert.application.result.AlertResult;
import java.util.List;

public interface AlertReadModel {

    List<AlertResult> findByProjectFiltered(Integer projectId, String type, String severity, String status);
}

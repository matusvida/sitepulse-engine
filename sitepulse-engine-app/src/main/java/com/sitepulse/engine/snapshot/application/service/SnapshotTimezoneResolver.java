package com.sitepulse.engine.snapshot.application.service;

import com.sitepulse.engine.project.domain.model.Project;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class SnapshotTimezoneResolver {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Bratislava");

    public ZoneId resolve(Project project) {
        if (project.getTimezone() == null || project.getTimezone().isBlank()) {
            return DEFAULT_ZONE;
        }
        return ZoneId.of(project.getTimezone());
    }
}

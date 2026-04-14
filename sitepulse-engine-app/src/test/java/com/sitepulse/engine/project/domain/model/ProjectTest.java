package com.sitepulse.engine.project.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectTest {

    @Test
    void createNormalizesTimezone() {
        Project project = Project.create("Project", "Location", "prefix", "Europe/Bratislava", NOW);

        assertEquals("Europe/Bratislava", project.getTimezone());
    }

    @Test
    void updateNormalizesTimezone() {
        Project project = Project.restore(1, "Project", "Location", "prefix", null, NOW);

        project.update(null, null, null, "Europe/Bratislava");

        assertEquals("Europe/Bratislava", project.getTimezone());
    }

    @Test
    void restoreKeepsNullTimezone() {
        Project project = Project.restore(1, "Project", "Location", "prefix", null, NOW);

        assertNull(project.getTimezone());
    }

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 4, 11, 12, 0, 0, 0, ZoneOffset.UTC);
}

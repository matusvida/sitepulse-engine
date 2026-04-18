package com.sitepulse.engine.plan.domain.model;

import com.sitepulse.engine.plan.domain.enums.PlanStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstructionPlanTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void uploadCreatesProcessingPlan() {
        ConstructionPlan plan = ConstructionPlan.upload(1, "plan.pdf", "raw text", NOW);
        assertEquals(PlanStatus.PROCESSING, plan.getStatus());
    }

    @Test
    void markReadyFromProcessingSucceeds() {
        ConstructionPlan plan = ConstructionPlan.upload(1, "plan.pdf", "raw text", NOW);
        plan.markReady(NOW.plusMinutes(1));
        assertEquals(PlanStatus.READY, plan.getStatus());
    }

    @Test
    void markReadyFromReadyFails() {
        ConstructionPlan plan = ConstructionPlan.upload(1, "plan.pdf", "raw text", NOW);
        plan.markReady(NOW.plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> plan.markReady(NOW.plusMinutes(2)));
    }

    @Test
    void markFailedFromProcessingSucceeds() {
        ConstructionPlan plan = ConstructionPlan.upload(1, "plan.pdf", "raw text", NOW);
        plan.markFailed(NOW.plusMinutes(1));
        assertEquals(PlanStatus.FAILED, plan.getStatus());
    }

    @Test
    void markFailedFromReadyFails() {
        ConstructionPlan plan = ConstructionPlan.upload(1, "plan.pdf", "raw text", NOW);
        plan.markReady(NOW.plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> plan.markFailed(NOW.plusMinutes(2)));
    }
}

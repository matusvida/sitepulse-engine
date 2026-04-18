package com.sitepulse.engine.plan.domain.model;

import com.sitepulse.engine.plan.domain.enums.MilestoneStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanMilestoneTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void createSetsNotStartedStatus() {
        PlanMilestone milestone = PlanMilestone.create(1, 1, 1, "Title", "Desc", "Expected", NOW);
        assertEquals(MilestoneStatus.NOT_STARTED, milestone.getStatus());
    }

    @Test
    void applyAssessmentUpdatesStatusAndState() {
        PlanMilestone milestone = PlanMilestone.create(1, 1, 1, "Title", "Desc", "Expected", NOW);
        MilestoneAssessment assessment = new MilestoneAssessment(MilestoneStatus.ON_TRACK, "Looking good");
        milestone.applyAssessment(assessment, NOW.plusDays(1));
        assertEquals(MilestoneStatus.ON_TRACK, milestone.getStatus());
        assertEquals("Looking good", milestone.getActualState());
    }

    @Test
    void applyAssessmentRejectsCompletedMilestone() {
        PlanMilestone milestone = PlanMilestone.create(1, 1, 1, "Title", "Desc", "Expected", NOW);
        milestone.applyAssessment(new MilestoneAssessment(MilestoneStatus.COMPLETED, "Done"), NOW.plusDays(1));
        assertThrows(IllegalStateException.class,
                () -> milestone.applyAssessment(new MilestoneAssessment(MilestoneStatus.DELAYED, "Late"), NOW.plusDays(2)));
    }

    @Test
    void updateDetailsOnlyChangesNonNullFields() {
        PlanMilestone milestone = PlanMilestone.create(1, 1, 1, "Title", "Desc", "Expected", NOW);
        milestone.updateDetails(null, "New Desc", null);
        assertEquals("Title", milestone.getTitle());
        assertEquals("New Desc", milestone.getDescription());
        assertEquals("Expected", milestone.getExpectedState());
    }
}

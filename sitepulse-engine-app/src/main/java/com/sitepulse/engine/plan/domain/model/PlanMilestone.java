package com.sitepulse.engine.plan.domain.model;

import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class PlanMilestone {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final Integer id;

    private final Integer planId;
    private final Integer projectId;

    @ToString.Include
    private final Integer weekNumber;

    @ToString.Include
    private String title;
    private String description;
    private String expectedState;
    private String actualState;
    private MilestoneStatus status;
    private OffsetDateTime checkedAt;
    private final OffsetDateTime createdAt;

    public static PlanMilestone create(
            Integer planId,
            Integer projectId,
            Integer weekNumber,
            String title,
            String description,
            String expectedState,
            OffsetDateTime createdAt
    ) {
        return new PlanMilestone(null, planId, projectId, weekNumber, title, description, expectedState, null, MilestoneStatus.NOT_STARTED, null, createdAt);
    }

    public static PlanMilestone restore(
            Integer id,
            Integer planId,
            Integer projectId,
            Integer weekNumber,
            String title,
            String description,
            String expectedState,
            String actualState,
            MilestoneStatus status,
            OffsetDateTime checkedAt,
            OffsetDateTime createdAt
    ) {
        return new PlanMilestone(id, planId, projectId, weekNumber, title, description, expectedState, actualState, status, checkedAt, createdAt);
    }

    public void updateDetails(String title, String description, String expectedState) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (expectedState != null) {
            this.expectedState = expectedState;
        }
    }

    public void updateStatus(MilestoneStatus status) {
        this.status = status;
    }

    public void applyAssessment(MilestoneAssessment assessment, OffsetDateTime checkedAt) {
        this.status = assessment.status();
        this.actualState = assessment.actualState();
        this.checkedAt = checkedAt;
    }
}

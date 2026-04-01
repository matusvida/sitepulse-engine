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
public class ConstructionPlan {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final Integer id;

    private final Integer projectId;
    private final String filename;
    private final String rawText;

    @ToString.Include
    private PlanStatus status;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ConstructionPlan upload(Integer projectId, String filename, String rawText, OffsetDateTime createdAt) {
        return new ConstructionPlan(null, projectId, filename, rawText, PlanStatus.PROCESSING, createdAt, null);
    }

    public static ConstructionPlan restore(
            Integer id,
            Integer projectId,
            String filename,
            String rawText,
            PlanStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        return new ConstructionPlan(id, projectId, filename, rawText, status, createdAt, updatedAt);
    }

    public void markReady(OffsetDateTime updatedAt) {
        if (status != PlanStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark plan ready from PROCESSING state, current: " + status);
        }
        this.status = PlanStatus.READY;
        this.updatedAt = updatedAt;
    }

    public void markFailed(OffsetDateTime updatedAt) {
        if (status != PlanStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark plan failed from PROCESSING state, current: " + status);
        }
        this.status = PlanStatus.FAILED;
        this.updatedAt = updatedAt;
    }
}

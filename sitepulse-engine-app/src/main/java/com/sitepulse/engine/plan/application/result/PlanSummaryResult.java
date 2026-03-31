package com.sitepulse.engine.plan.application.result;

import com.sitepulse.engine.plan.domain.model.PlanStatus;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PlanSummaryResult {

    private Integer id;
    private String filename;
    private PlanStatus status;
    private OffsetDateTime createdAt;
    private List<PlanMilestoneResult> milestones;
}

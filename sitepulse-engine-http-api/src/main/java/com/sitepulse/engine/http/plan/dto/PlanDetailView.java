package com.sitepulse.engine.http.plan.dto;

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
public class PlanDetailView {

    private PlanInfoView plan;
    private List<PlanMilestoneView> milestones;
}

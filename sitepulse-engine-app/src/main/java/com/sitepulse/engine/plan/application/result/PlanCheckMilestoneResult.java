package com.sitepulse.engine.plan.application.result;

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
public class PlanCheckMilestoneResult {

    private Integer milestoneId;
    private String title;
    private String status;
    private String actualState;
    private String error;
}

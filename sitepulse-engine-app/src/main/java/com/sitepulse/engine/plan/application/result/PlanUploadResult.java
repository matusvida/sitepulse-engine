package com.sitepulse.engine.plan.application.result;

import com.sitepulse.engine.plan.domain.enums.PlanStatus;
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
public class PlanUploadResult {

    private Integer planId;
    private String filename;
    private int milestonesCreated;
    private PlanStatus status;
}

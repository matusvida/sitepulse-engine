package com.sitepulse.engine.plan.application.result;

import com.sitepulse.engine.plan.domain.enums.MilestoneStatus;
import java.time.OffsetDateTime;
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
public class PlanMilestoneResult {

    private Integer id;
    private Integer weekNumber;
    private String title;
    private String description;
    private String expectedState;
    private String actualState;
    private MilestoneStatus status;
    private OffsetDateTime checkedAt;
    private OffsetDateTime createdAt;
}

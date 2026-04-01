package com.sitepulse.engine.http.plan.dto;

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
public class PlanMilestoneView {

    private Integer id;
    private Integer weekNumber;
    private String title;
    private String description;
    private String expectedState;
    private String actualState;
    private String status;
    private String checkedAt;
    private String createdAt;
}

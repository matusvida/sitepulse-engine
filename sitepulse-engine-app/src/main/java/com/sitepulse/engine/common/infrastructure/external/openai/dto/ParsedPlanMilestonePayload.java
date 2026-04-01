package com.sitepulse.engine.common.infrastructure.external.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ParsedPlanMilestonePayload {

    @JsonProperty("week_number")
    private int weekNumber;

    private String title;
    private String description;

    @JsonProperty("expected_state")
    private String expectedState;
}

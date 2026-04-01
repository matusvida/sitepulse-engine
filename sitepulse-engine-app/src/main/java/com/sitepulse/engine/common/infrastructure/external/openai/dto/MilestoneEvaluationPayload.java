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
public class MilestoneEvaluationPayload {

    private String status;

    @JsonProperty("actual_state")
    private String actualState;

    private Double confidence;
}

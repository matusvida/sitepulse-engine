package com.sitepulse.engine.common.infrastructure.external.openai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenAiChatRequest {

    private String model;
    private List<Map<String, Object>> messages;
    private Map<String, Object> responseFormat;
    private Double temperature;
    private Integer maxTokens;
}

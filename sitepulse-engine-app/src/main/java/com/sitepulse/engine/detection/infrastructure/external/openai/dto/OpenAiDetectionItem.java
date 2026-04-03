package com.sitepulse.engine.detection.infrastructure.external.openai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenAiDetectionItem {

    private Integer classId;
    private String className;
    private Double score;
    private List<Double> bboxXyxy;
    private String colorHint;
    private String notes;
    private String sameOrUnique;
    private Integer matchedTrackId;
}

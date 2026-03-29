package com.sitepulse.engine.integration.yolo.dto;

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
public class YoloInferResponse {

    private String modelVersion;
    private Integer imageWidth;
    private Integer imageHeight;
    private Double inferenceMs;
    private List<YoloRawDetection> rawDetections;
}

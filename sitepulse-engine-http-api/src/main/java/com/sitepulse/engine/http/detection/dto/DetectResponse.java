package com.sitepulse.engine.http.detection.dto;

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
public class DetectResponse {

    private String modelVersion;
    private String bucket;
    private String key;
    private Integer imageWidth;
    private Integer imageHeight;
    private Double inferenceMs;
    private List<DetectionView> detections;
    private List<String> warnings;
}

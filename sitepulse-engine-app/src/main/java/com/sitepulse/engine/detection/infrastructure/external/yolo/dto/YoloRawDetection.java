package com.sitepulse.engine.detection.infrastructure.external.yolo.dto;

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
public class YoloRawDetection {

    private Integer classId;
    private String className;
    private Double score;
    private List<Double> bboxXyxy;
}

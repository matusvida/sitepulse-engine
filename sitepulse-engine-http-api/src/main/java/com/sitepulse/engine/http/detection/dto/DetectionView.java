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
public class DetectionView {

    private Integer classId;
    private String className;
    private Double score;
    private List<Double> bboxXyxy;
    private Boolean inRoi;
    private Integer trackId;
    private String colorHint;
    private String notes;
}

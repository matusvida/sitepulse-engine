package com.sitepulse.engine.integration.yolo.dto;

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
public class YoloHealthResponse {

    private String status;
    private Boolean modelLoaded;
    private String modelVersion;
}

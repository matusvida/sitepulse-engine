package com.sitepulse.engine.metrics.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
public class DetectionActivitySample {

    private String className;
    private Integer imageId;
    private OffsetDateTime capturedAt;
    private LocalDate capturedDate;
}

package com.sitepulse.engine.http.project.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CameraCreateRequest {

    @NotBlank
    private String name;

    private String keyPrefix;
    private List<List<Double>> roiPolygon;
    private Boolean dropOutside;
}

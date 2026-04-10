package com.sitepulse.engine.http.project.dto;

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
public class CameraView {

    private Integer id;
    private Integer projectId;
    private String name;
    private String dropboxPath;
    private List<List<Double>> roiPolygon;
    private Boolean dropOutside;
    private String keyPrefix;
    private String createdAt;
}

package com.sitepulse.engine.http.project.dto;

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
public class ProjectView {

    private String id;
    private String name;
    private String location;
    private Integer coveragePercent;
    private Integer cameraCount;
    private String lastSnapshotAt;
    private String storageKeyPrefix;
    private String createdAt;
}

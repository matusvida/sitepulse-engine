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
public class SyncStatusView {

    private String id;
    private String projectId;
    private String status;
    private String message;
    private Integer imagesFound;
    private Integer imagesSynced;
    private String error;
    private String startedAt;
    private String finishedAt;
}

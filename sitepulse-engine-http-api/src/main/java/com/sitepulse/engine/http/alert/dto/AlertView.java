package com.sitepulse.engine.http.alert.dto;

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
public class AlertView {

    private Integer id;
    private Integer projectId;
    private String type;
    private String severity;
    private String status;
    private String summary;
    private String details;
    private List<String> recommendedActions;
    private String createdAt;
    private String updatedAt;
}

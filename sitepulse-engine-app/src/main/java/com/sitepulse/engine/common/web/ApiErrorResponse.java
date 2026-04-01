package com.sitepulse.engine.common.web;

import java.time.OffsetDateTime;
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
public class ApiErrorResponse {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String code;
    private String detail;
    private String path;
    private List<ApiViolation> violations;
}

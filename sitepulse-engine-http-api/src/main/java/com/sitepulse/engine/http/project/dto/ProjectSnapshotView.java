package com.sitepulse.engine.http.project.dto;

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
public class ProjectSnapshotView {

    private String date;
    private String url;
    private OffsetDateTime expiresAt;
    private String mediaType;
}

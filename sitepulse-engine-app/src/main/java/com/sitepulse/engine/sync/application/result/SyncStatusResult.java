package com.sitepulse.engine.sync.application.result;

import com.sitepulse.engine.sync.domain.enums.SyncJobStatus;
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
public class SyncStatusResult {

    private Integer jobId;
    private Integer projectId;
    private SyncJobStatus status;
    private String message;
    private Integer imagesFound;
    private Integer imagesSynced;
    private String error;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private boolean neverRun;

    public static SyncStatusResult neverRun(Integer projectId) {
        return new SyncStatusResult(null, projectId, null, "No sync jobs have been run for this project", null, null, null, null, null, true);
    }
}

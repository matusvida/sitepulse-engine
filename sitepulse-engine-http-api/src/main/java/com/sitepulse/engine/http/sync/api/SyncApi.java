package com.sitepulse.engine.http.sync.api;

import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.project.dto.SyncStatusView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Sync")
@RequestMapping("/api")
public interface SyncApi {

    @Operation(summary = "Get latest sync status")
    @GetMapping("/projects/{projectId}/sync/status")
    SyncStatusView syncStatus(@PathVariable Integer projectId);

    @Operation(summary = "Trigger a project sync")
    @PostMapping("/projects/{projectId}/sync/trigger")
    ActionResponse triggerSync(@PathVariable Integer projectId);
}

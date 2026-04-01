package com.sitepulse.engine.http.alert.api;

import com.sitepulse.engine.http.alert.dto.AlertStatusUpdateRequest;
import com.sitepulse.engine.http.alert.dto.AlertView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Alerts")
@RequestMapping("/api")
public interface AlertApi {

    @Operation(summary = "List alerts")
    @GetMapping("/projects/{projectId}/alerts")
    List<AlertView> listAlerts(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status
    );

    @Operation(summary = "Update alert status")
    @PatchMapping("/projects/{projectId}/alerts/{alertId}")
    AlertView updateAlert(@PathVariable Integer projectId, @PathVariable Integer alertId, @Valid @RequestBody AlertStatusUpdateRequest request);
}

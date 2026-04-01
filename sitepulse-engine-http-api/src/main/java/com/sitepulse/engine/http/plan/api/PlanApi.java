package com.sitepulse.engine.http.plan.api;

import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.plan.dto.PlanCheckView;
import com.sitepulse.engine.http.plan.dto.PlanDetailView;
import com.sitepulse.engine.http.plan.dto.PlanMilestoneView;
import com.sitepulse.engine.http.plan.dto.PlanUploadView;
import com.sitepulse.engine.http.plan.dto.MilestoneUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Plans")
@RequestMapping("/api/projects/{projectId}/plan")
public interface PlanApi {

    @Operation(summary = "Upload a construction plan PDF")
    @PostMapping("/upload")
    PlanUploadView upload(@PathVariable Integer projectId, @RequestPart("file") MultipartFile file);

    @Operation(summary = "Get the latest uploaded plan")
    @GetMapping
    PlanDetailView getPlan(@PathVariable Integer projectId);

    @Operation(summary = "List plan milestones")
    @GetMapping("/milestones")
    List<PlanMilestoneView> listMilestones(@PathVariable Integer projectId);

    @Operation(summary = "Update a plan milestone")
    @PatchMapping("/milestones/{milestoneId}")
    ActionResponse updateMilestone(
            @PathVariable Integer projectId,
            @PathVariable Integer milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request
    );

    @Operation(summary = "Run a plan progress check")
    @PostMapping("/check")
    PlanCheckView check(@PathVariable Integer projectId);
}

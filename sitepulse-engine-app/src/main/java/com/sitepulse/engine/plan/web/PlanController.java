package com.sitepulse.engine.plan.web;

import com.sitepulse.engine.http.plan.api.PlanApi;
import com.sitepulse.engine.http.plan.dto.MilestoneUpdateRequest;
import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.plan.application.command.UpdatePlanMilestoneCommand;
import com.sitepulse.engine.plan.application.result.PlanCheckResult;
import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.application.result.PlanUploadResult;
import com.sitepulse.engine.plan.application.usecase.GetLatestPlanQuery;
import com.sitepulse.engine.plan.application.usecase.ListPlanMilestonesQuery;
import com.sitepulse.engine.plan.application.usecase.RunPlanCheckUseCase;
import com.sitepulse.engine.plan.application.usecase.UpdatePlanMilestoneUseCase;
import com.sitepulse.engine.plan.application.usecase.UploadConstructionPlanUseCase;
import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PlanController implements PlanApi {

    private final UploadConstructionPlanUseCase uploadConstructionPlanUseCase;
    private final GetLatestPlanQuery getLatestPlanQuery;
    private final ListPlanMilestonesQuery listPlanMilestonesQuery;
    private final UpdatePlanMilestoneUseCase updatePlanMilestoneUseCase;
    private final RunPlanCheckUseCase runPlanCheckUseCase;

    @Override
    public Map<String, Object> upload(Integer projectId, MultipartFile file) {
        PlanUploadResult result = uploadConstructionPlanUseCase.upload(projectId, file);
        return Map.of(
                "planId", result.getPlanId(),
                "filename", result.getFilename(),
                "milestonesCreated", result.getMilestonesCreated(),
                "status", result.getStatus().toPersistenceValue()
        );
    }

    @Override
    public Map<String, Object> getPlan(Integer projectId) {
        return getLatestPlanQuery.get(projectId)
                .<Map<String, Object>>map(result -> {
                    Map<String, Object> plan = new HashMap<>();
                    plan.put("id", result.getId());
                    plan.put("filename", result.getFilename());
                    plan.put("status", result.getStatus().toPersistenceValue());
                    plan.put("createdAt", result.getCreatedAt() == null ? null : result.getCreatedAt().toString());

                    Map<String, Object> response = new HashMap<>();
                    response.put("plan", plan);
                    response.put("milestones", result.getMilestones().stream().map(this::toMilestoneView).toList());
                    return response;
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("plan", null);
                    response.put("milestones", List.of());
                    return response;
                });
    }

    @Override
    public List<Map<String, Object>> listMilestones(Integer projectId) {
        return listPlanMilestonesQuery.list(projectId).stream().map(this::toMilestoneView).toList();
    }

    @Override
    public Map<String, Object> updateMilestone(
            Integer projectId,
            Integer milestoneId,
            MilestoneUpdateRequest request
    ) {
        updatePlanMilestoneUseCase.update(new UpdatePlanMilestoneCommand(
                projectId,
                milestoneId,
                request.getTitle(),
                request.getDescription(),
                request.getExpectedState(),
                parseStatus(request.getStatus())
        ));
        return Map.of("ok", true);
    }

    @Override
    public Map<String, Object> check(Integer projectId) {
        PlanCheckResult result = runPlanCheckUseCase.run(projectId);
        Map<String, Object> response = new HashMap<>();
        response.put("milestonesChecked", result.getMilestonesChecked());
        response.put("results", result.getResults().stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("milestoneId", item.getMilestoneId());
            row.put("title", item.getTitle());
            row.put("status", item.getStatus());
            row.put("actualState", item.getActualState());
            row.put("error", item.getError());
            return row;
        }).toList());
        return response;
    }

    private MilestoneStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MilestoneStatus.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status. Must be one of: [not_started, on_track, delayed, completed]");
        }
    }

    private Map<String, Object> toMilestoneView(PlanMilestoneResult milestone) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", milestone.getId());
        row.put("weekNumber", milestone.getWeekNumber());
        row.put("title", milestone.getTitle());
        row.put("description", milestone.getDescription() == null ? "" : milestone.getDescription());
        row.put("expectedState", milestone.getExpectedState() == null ? "" : milestone.getExpectedState());
        row.put("actualState", milestone.getActualState());
        row.put("status", milestone.getStatus().toPersistenceValue());
        row.put("checkedAt", milestone.getCheckedAt() == null ? null : milestone.getCheckedAt().toString());
        row.put("createdAt", milestone.getCreatedAt() == null ? null : milestone.getCreatedAt().toString());
        return row;
    }
}

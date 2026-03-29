package com.sitepulse.engine.plan.web;

import com.sitepulse.engine.http.plan.api.PlanApi;
import com.sitepulse.engine.http.plan.dto.MilestoneUpdateRequest;
import com.sitepulse.engine.plan.application.PlanService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PlanController implements PlanApi {

    private final PlanService planService;

    @Override
    public Map<String, Object> upload(Integer projectId, MultipartFile file) {
        return planService.uploadPlan(projectId, file);
    }

    @Override
    public Map<String, Object> getPlan(Integer projectId) {
        return planService.getLatestPlan(projectId);
    }

    @Override
    public List<Map<String, Object>> listMilestones(Integer projectId) {
        return planService.listMilestones(projectId);
    }

    @Override
    public Map<String, Object> updateMilestone(
            Integer projectId,
            Integer milestoneId,
            MilestoneUpdateRequest request
    ) {
        return planService.updateMilestone(projectId, milestoneId, request);
    }

    @Override
    public Map<String, Object> check(Integer projectId) {
        return planService.runPlanCheck(projectId);
    }
}

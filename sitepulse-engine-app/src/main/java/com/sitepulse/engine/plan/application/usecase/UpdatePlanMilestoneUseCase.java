package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.plan.application.PlanResultMapper;
import com.sitepulse.engine.plan.application.command.UpdatePlanMilestoneCommand;
import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.port.PlanMilestoneCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePlanMilestoneUseCase {

    private final PlanMilestoneCatalogRepository planMilestoneCatalogRepository;
    private final PlanResultMapper planResultMapper;

    @Transactional
    public PlanMilestoneResult update(UpdatePlanMilestoneCommand command) {
        PlanMilestone milestone = planMilestoneCatalogRepository.findByIdAndProjectId(command.milestoneId(), command.projectId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Milestone not found"));
        milestone.updateDetails(command.title(), command.description(), command.expectedState());
        if (command.status() != null) {
            milestone.updateStatus(command.status());
        }
        return planResultMapper.toMilestoneResult(planMilestoneCatalogRepository.save(milestone));
    }
}

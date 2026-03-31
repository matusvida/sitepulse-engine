package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.alert.application.command.CreateAlertCommand;
import com.sitepulse.engine.alert.application.usecase.CreateAlertUseCase;
import com.sitepulse.engine.alert.domain.model.AlertSeverity;
import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.plan.application.result.PlanCheckMilestoneResult;
import com.sitepulse.engine.plan.application.result.PlanCheckResult;
import com.sitepulse.engine.plan.domain.model.MilestoneAssessment;
import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.model.PlanStatus;
import com.sitepulse.engine.plan.domain.port.ConstructionPlanCatalogRepository;
import com.sitepulse.engine.plan.domain.port.PlanEvidenceImageProvider;
import com.sitepulse.engine.plan.domain.port.PlanIntelligenceGateway;
import com.sitepulse.engine.plan.domain.port.PlanMilestoneCatalogRepository;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunPlanCheckUseCase {

    private final ProjectLookupService projectLookupService;
    private final ConstructionPlanCatalogRepository constructionPlanCatalogRepository;
    private final PlanMilestoneCatalogRepository planMilestoneCatalogRepository;
    private final PlanEvidenceImageProvider planEvidenceImageProvider;
    private final PlanIntelligenceGateway planIntelligenceGateway;
    private final CreateAlertUseCase createAlertUseCase;

    public PlanCheckResult run(Integer projectId) {
        projectLookupService.requireProject(projectId);
        log.info("Running plan check for projectId={}", projectId);
        Integer planId = constructionPlanCatalogRepository.findLatestByProjectIdAndStatus(projectId, PlanStatus.READY)
                .map(plan -> plan.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No ready plan found for this project"));
        List<PlanMilestone> milestones = planMilestoneCatalogRepository.findByPlanIdAndStatusNot(planId, MilestoneStatus.COMPLETED);
        List<byte[]> images = planEvidenceImageProvider.recentProjectImages(projectId, 5);
        if (images.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No recent images available for evaluation");
        }
        List<PlanCheckMilestoneResult> results = milestones.stream()
                .map(milestone -> evaluateMilestone(projectId, milestone, images))
                .toList();
        return new PlanCheckResult(results.size(), results);
    }

    private PlanCheckMilestoneResult evaluateMilestone(Integer projectId, PlanMilestone milestone, List<byte[]> images) {
        try {
            MilestoneAssessment assessment = planIntelligenceGateway.evaluateMilestone(milestone, images);
            milestone.applyAssessment(assessment, OffsetDateTime.now(ZoneOffset.UTC));
            planMilestoneCatalogRepository.save(milestone);
            if (assessment.status() == MilestoneStatus.DELAYED) {
                createAlertUseCase.create(new CreateAlertCommand(
                        projectId,
                        "schedule",
                        AlertSeverity.HIGH,
                        "Schedule delay: Week " + milestone.getWeekNumber() + " - " + milestone.getTitle(),
                        "Milestone is behind schedule. Current assessment: " + assessment.actualState(),
                        List.of("Review milestone expectations", "Reallocate resources", "Schedule a site visit")
                ));
            }
            return new PlanCheckMilestoneResult(
                    milestone.getId(),
                    milestone.getTitle(),
                    assessment.status().toPersistenceValue(),
                    assessment.actualState(),
                    null
            );
        } catch (Exception ex) {
            return new PlanCheckMilestoneResult(milestone.getId(), milestone.getTitle(), "error", null, ex.getMessage());
        }
    }
}

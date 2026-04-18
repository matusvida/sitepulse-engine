package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.plan.application.result.PlanCheckMilestoneResult;
import com.sitepulse.engine.plan.application.result.PlanCheckResult;
import com.sitepulse.engine.plan.domain.event.MilestoneDelayedEvent;
import com.sitepulse.engine.plan.domain.model.MilestoneAssessment;
import com.sitepulse.engine.plan.domain.enums.MilestoneStatus;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.enums.PlanStatus;
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
    private final DomainEventPublisher domainEventPublisher;

    public PlanCheckResult run(Integer projectId) {
        projectLookupService.requireProject(projectId);
        log.info("Running plan check for projectId={}", projectId);
        Integer planId = constructionPlanCatalogRepository.findLatestByProjectIdAndStatus(projectId, PlanStatus.READY)
                .map(plan -> plan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No ready plan found for this project"));
        List<PlanMilestone> milestones = planMilestoneCatalogRepository.findByPlanIdAndStatusNot(planId, MilestoneStatus.COMPLETED);
        List<byte[]> images = planEvidenceImageProvider.recentProjectImages(projectId, 5);
        if (images.isEmpty()) {
            throw new ProcessingException("No recent images available for evaluation");
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
                domainEventPublisher.publish(new MilestoneDelayedEvent(
                        projectId,
                        milestone.getId(),
                        milestone.getWeekNumber(),
                        milestone.getTitle(),
                        assessment.actualState()
                ));
            }
            return new PlanCheckMilestoneResult(
                    milestone.getId(),
                    milestone.getTitle(),
                    assessment.status().toPersistenceValue(),
                    assessment.actualState(),
                    null
            );
        } catch (RuntimeException ex) {
            return new PlanCheckMilestoneResult(milestone.getId(), milestone.getTitle(), "error", null, ex.getMessage());
        }
    }
}

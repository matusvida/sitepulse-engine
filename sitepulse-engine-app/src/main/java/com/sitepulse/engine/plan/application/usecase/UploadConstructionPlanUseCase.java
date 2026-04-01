package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.plan.application.command.UploadPlanCommand;
import com.sitepulse.engine.plan.application.result.PlanUploadResult;
import com.sitepulse.engine.plan.domain.model.ConstructionPlan;
import com.sitepulse.engine.plan.domain.model.ParsedMilestone;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.port.ConstructionPlanCatalogRepository;
import com.sitepulse.engine.plan.domain.port.PlanDocumentTextExtractor;
import com.sitepulse.engine.plan.domain.port.PlanIntelligenceGateway;
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
public class UploadConstructionPlanUseCase {

    private final ProjectLookupService projectLookupService;
    private final PlanDocumentTextExtractor planDocumentTextExtractor;
    private final PlanIntelligenceGateway planIntelligenceGateway;
    private final ConstructionPlanCatalogRepository constructionPlanCatalogRepository;

    public PlanUploadResult upload(UploadPlanCommand command) {
        projectLookupService.requireProject(command.projectId());
        log.info("Uploading construction plan for projectId={} filename={}", command.projectId(), command.filename());
        try {
            String rawText = planDocumentTextExtractor.extract(command.content());
            List<ParsedMilestone> parsedMilestones = planIntelligenceGateway.parseMilestones(rawText);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            ConstructionPlan plan = ConstructionPlan.upload(command.projectId(), command.filename(), rawText, now);
            plan.markReady(now);
            List<PlanMilestone> milestones = parsedMilestones.stream()
                    .map(milestone -> PlanMilestone.create(
                            plan.getId(),
                            command.projectId(),
                            milestone.weekNumber(),
                            milestone.title(),
                            milestone.description(),
                            milestone.expectedState(),
                            now
                    ))
                    .toList();
            ConstructionPlan savedPlan = constructionPlanCatalogRepository.saveUploadedPlan(plan, milestones);
            return new PlanUploadResult(savedPlan.getId(), savedPlan.getFilename(), parsedMilestones.size(), savedPlan.getStatus());
        } catch (RuntimeException ex) {
            log.error("Plan upload failed for projectId={} filename={}", command.projectId(), command.filename(), ex);
            throw new ExternalServiceException("Plan upload failed", ex);
        }
    }
}

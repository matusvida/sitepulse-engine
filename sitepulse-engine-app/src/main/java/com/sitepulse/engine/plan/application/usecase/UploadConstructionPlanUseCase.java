package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.common.exception.ValidationException;
import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadConstructionPlanUseCase {

    private static final long MAX_PDF_SIZE = 20L * 1024 * 1024;

    private final ProjectLookupService projectLookupService;
    private final PlanDocumentTextExtractor planDocumentTextExtractor;
    private final PlanIntelligenceGateway planIntelligenceGateway;
    private final ConstructionPlanCatalogRepository constructionPlanCatalogRepository;

    public PlanUploadResult upload(Integer projectId, MultipartFile file) {
        projectLookupService.requireProject(projectId);
        validateFile(file);
        log.info("Uploading construction plan for projectId={} filename={}", projectId, file.getOriginalFilename());
        try {
            String rawText = planDocumentTextExtractor.extract(file.getBytes());
            List<ParsedMilestone> parsedMilestones = planIntelligenceGateway.parseMilestones(rawText);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            ConstructionPlan plan = ConstructionPlan.upload(projectId, file.getOriginalFilename(), rawText, now);
            plan.markReady(now);
            List<PlanMilestone> milestones = parsedMilestones.stream()
                    .map(milestone -> PlanMilestone.create(
                            plan.getId(),
                            projectId,
                            milestone.weekNumber(),
                            milestone.title(),
                            milestone.description(),
                            milestone.expectedState(),
                            now
                    ))
                    .toList();
            ConstructionPlan savedPlan = constructionPlanCatalogRepository.saveUploadedPlan(plan, milestones);
            return new PlanUploadResult(savedPlan.getId(), savedPlan.getFilename(), parsedMilestones.size(), savedPlan.getStatus());
        } catch (IOException ex) {
            log.error("Plan upload failed for projectId={} filename={}", projectId, file.getOriginalFilename(), ex);
            throw new ExternalServiceException("Plan upload failed", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Only PDF files are accepted");
        }
        if (file.getSize() > MAX_PDF_SIZE) {
            throw new ValidationException("File too large (max 20 MB)");
        }
    }
}

package com.sitepulse.engine.plan.application;

import com.sitepulse.engine.alert.application.AlertService;
import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.http.plan.dto.MilestoneUpdateRequest;
import com.sitepulse.engine.integration.openai.OpenAiService;
import com.sitepulse.engine.integration.pdf.PdfTextExtractor;
import com.sitepulse.engine.integration.storage.StorageService;
import com.sitepulse.engine.plan.domain.ConstructionPlanEntity;
import com.sitepulse.engine.plan.domain.PlanMilestoneEntity;
import com.sitepulse.engine.plan.persistence.ConstructionPlanRepository;
import com.sitepulse.engine.plan.persistence.PlanMilestoneRepository;
import com.sitepulse.engine.project.application.ProjectService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {

    private static final long MAX_PDF_SIZE = 20L * 1024 * 1024;

    private final ProjectService projectService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OpenAiService openAiService;
    private final ConstructionPlanRepository constructionPlanRepository;
    private final PlanMilestoneRepository planMilestoneRepository;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final AlertService alertService;

    @Transactional
    public Map<String, Object> uploadPlan(Integer projectId, MultipartFile file) {
        projectService.requireProject(projectId);
        log.info("Uploading construction plan for projectId={} filename={}", projectId, file.getOriginalFilename());
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted");
        }
        if (file.getSize() > MAX_PDF_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File too large (max 20 MB)");
        }
        try {
            String rawText = pdfTextExtractor.extract(file.getBytes());
            ConstructionPlanEntity plan = constructionPlanRepository.save(ConstructionPlanEntity.builder()
                    .projectId(projectId)
                    .filename(file.getOriginalFilename())
                    .rawText(rawText)
                    .status("processing")
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
            List<Map<String, Object>> milestones = openAiService.parsePlanMilestones(rawText);
            for (Map<String, Object> milestone : milestones) {
                planMilestoneRepository.save(PlanMilestoneEntity.builder()
                        .planId(plan.getId())
                        .projectId(projectId)
                        .weekNumber(((Number) milestone.getOrDefault("week_number", 0)).intValue())
                        .title(String.valueOf(milestone.getOrDefault("title", "Untitled")))
                        .description(String.valueOf(milestone.getOrDefault("description", "")))
                        .expectedState(String.valueOf(milestone.getOrDefault("expected_state", "")))
                        .status("not_started")
                        .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .build());
            }
            plan.setStatus("ready");
            plan.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            constructionPlanRepository.save(plan);
            log.info("Plan upload completed for projectId={} planId={} milestones={}", projectId, plan.getId(), milestones.size());
            return Map.of("planId", plan.getId(), "filename", plan.getFilename(), "milestonesCreated", milestones.size(), "status", "ready");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Plan upload failed for projectId={} filename={}", projectId, file.getOriginalFilename(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Plan upload failed");
        }
    }

    public Map<String, Object> getLatestPlan(Integer projectId) {
        projectService.requireProject(projectId);
        ConstructionPlanEntity plan = constructionPlanRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
        if (plan == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("plan", null);
            response.put("milestones", List.of());
            return response;
        }
        Map<String, Object> planView = new HashMap<>();
        planView.put("id", plan.getId());
        planView.put("filename", plan.getFilename());
        planView.put("status", plan.getStatus());
        planView.put("createdAt", plan.getCreatedAt() == null ? null : plan.getCreatedAt().toString());
        Map<String, Object> response = new HashMap<>();
        response.put("plan", planView);
        response.put("milestones", milestonesForPlan(plan.getId()));
        return response;
    }

    public List<Map<String, Object>> listMilestones(Integer projectId) {
        projectService.requireProject(projectId);
        ConstructionPlanEntity plan = constructionPlanRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
        return plan == null ? List.of() : milestonesForPlan(plan.getId());
    }

    @Transactional
    public Map<String, Object> updateMilestone(Integer projectId, Integer milestoneId, MilestoneUpdateRequest request) {
        projectService.requireProject(projectId);
        PlanMilestoneEntity entity = planMilestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Milestone not found"));
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getExpectedState() != null) {
            entity.setExpectedState(request.getExpectedState());
        }
        if (request.getStatus() != null) {
            if (!List.of("not_started", "on_track", "delayed", "completed").contains(request.getStatus())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status. Must be one of: [not_started, on_track, delayed, completed]");
            }
            entity.setStatus(request.getStatus());
        }
        planMilestoneRepository.save(entity);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> runPlanCheck(Integer projectId) {
        projectService.requireProject(projectId);
        log.info("Running plan check for projectId={}", projectId);
        ConstructionPlanEntity plan = constructionPlanRepository.findTopByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "ready")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No ready plan found for this project"));
        List<PlanMilestoneEntity> milestones = planMilestoneRepository.findByPlanIdAndStatusNotOrderByWeekNumberAsc(plan.getId(), "completed");
        List<byte[]> images = recentImages(projectId, 5);
        if (images.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No recent images available for evaluation");
        }
        List<Map<String, Object>> results = milestones.stream().map(milestone -> evaluateMilestone(projectId, milestone, images)).toList();
        log.info("Plan check finished for projectId={} milestonesChecked={}", projectId, results.size());
        return Map.of("milestonesChecked", results.size(), "results", results);
    }

    private Map<String, Object> evaluateMilestone(Integer projectId, PlanMilestoneEntity milestone, List<byte[]> images) {
        try {
            Map<String, Object> assessment = openAiService.evaluateMilestone(milestone.getTitle(), milestone.getExpectedState() == null ? "" : milestone.getExpectedState(), images);
            String status = String.valueOf(assessment.getOrDefault("status", "not_started"));
            String actualState = String.valueOf(assessment.getOrDefault("actual_state", ""));
            milestone.setStatus(status);
            milestone.setActualState(actualState);
            milestone.setCheckedAt(OffsetDateTime.now(ZoneOffset.UTC));
            planMilestoneRepository.save(milestone);
            if ("delayed".equals(status)) {
                alertService.createAlert(projectId, "schedule", "high",
                        "Schedule delay: Week " + milestone.getWeekNumber() + " - " + milestone.getTitle(),
                        "Milestone is behind schedule. Current assessment: " + actualState,
                        List.of("Review milestone expectations", "Reallocate resources", "Schedule a site visit"));
            }
            return Map.of("milestoneId", milestone.getId(), "title", milestone.getTitle(), "status", status, "actualState", actualState);
        } catch (Exception ex) {
            return Map.of("milestoneId", milestone.getId(), "title", milestone.getTitle(), "status", "error", "error", ex.getMessage());
        }
    }

    private List<Map<String, Object>> milestonesForPlan(Integer planId) {
        return planMilestoneRepository.findByPlanIdOrderByWeekNumberAsc(planId).stream()
                .map(milestone -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", milestone.getId());
                    row.put("weekNumber", milestone.getWeekNumber());
                    row.put("title", milestone.getTitle());
                    row.put("description", milestone.getDescription());
                    row.put("expectedState", milestone.getExpectedState());
                    row.put("actualState", milestone.getActualState());
                    row.put("status", milestone.getStatus());
                    row.put("checkedAt", milestone.getCheckedAt() == null ? null : milestone.getCheckedAt().toString());
                    row.put("createdAt", milestone.getCreatedAt() == null ? null : milestone.getCreatedAt().toString());
                    return row;
                })
                .toList();
    }

    private List<byte[]> recentImages(Integer projectId, int limit) {
        return imageRepository.findProcessedByProject(projectId).stream()
                .limit(limit)
                .map(image -> storageService.download(image.getBucket(), image.getKey()))
                .toList();
    }
}

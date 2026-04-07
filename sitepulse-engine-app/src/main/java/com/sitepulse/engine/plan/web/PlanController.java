package com.sitepulse.engine.plan.web;

import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.plan.api.PlanApi;
import com.sitepulse.engine.http.plan.dto.MilestoneUpdateRequest;
import com.sitepulse.engine.http.plan.dto.PlanCheckItemView;
import com.sitepulse.engine.http.plan.dto.PlanCheckView;
import com.sitepulse.engine.http.plan.dto.PlanDetailView;
import com.sitepulse.engine.http.plan.dto.PlanInfoView;
import com.sitepulse.engine.http.plan.dto.PlanMilestoneView;
import com.sitepulse.engine.http.plan.dto.PlanUploadView;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.plan.application.command.UpdatePlanMilestoneCommand;
import com.sitepulse.engine.plan.application.command.UploadPlanCommand;
import com.sitepulse.engine.plan.application.result.PlanCheckResult;
import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.application.result.PlanUploadResult;
import com.sitepulse.engine.plan.application.usecase.GetLatestPlanQuery;
import com.sitepulse.engine.plan.application.usecase.ListPlanMilestonesQuery;
import com.sitepulse.engine.plan.application.usecase.RunPlanCheckUseCase;
import com.sitepulse.engine.plan.application.usecase.UpdatePlanMilestoneUseCase;
import com.sitepulse.engine.plan.application.usecase.UploadConstructionPlanUseCase;
import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PlanController implements PlanApi {

    private static final long MAX_PDF_SIZE = 20L * 1024 * 1024;

    private final UploadConstructionPlanUseCase uploadConstructionPlanUseCase;
    private final GetLatestPlanQuery getLatestPlanQuery;
    private final ListPlanMilestonesQuery listPlanMilestonesQuery;
    private final UpdatePlanMilestoneUseCase updatePlanMilestoneUseCase;
    private final RunPlanCheckUseCase runPlanCheckUseCase;

    @Override
    public PlanUploadView upload(Integer projectId, MultipartFile file) {
        validateFile(file);
        try {
            UploadPlanCommand command = new UploadPlanCommand(projectId, file.getOriginalFilename(), file.getBytes());
            PlanUploadResult result = uploadConstructionPlanUseCase.upload(command);
            return new PlanUploadView(
                    result.getPlanId(),
                    result.getFilename(),
                    result.getMilestonesCreated(),
                    result.getStatus().toPersistenceValue()
            );
        } catch (IOException ex) {
            throw new ValidationException("Failed to read uploaded file");
        }
    }

    @Override
    public PlanDetailView getPlan(Integer projectId) {
        return getLatestPlanQuery.get(projectId)
                .map(result -> new PlanDetailView(
                        new PlanInfoView(
                                result.getId(),
                                result.getFilename(),
                                result.getStatus().toPersistenceValue(),
                                result.getCreatedAt() == null ? null : result.getCreatedAt().toString()
                        ),
                        result.getMilestones().stream().map(this::toMilestoneView).toList()
                ))
                .orElseGet(() -> new PlanDetailView(null, List.of()));
    }

    @Override
    public List<PlanMilestoneView> listMilestones(Integer projectId) {
        return listPlanMilestonesQuery.list(projectId).stream().map(this::toMilestoneView).toList();
    }

    @Override
    public ActionResponse updateMilestone(
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
        return new ActionResponse("ok", "Milestone updated", projectId);
    }

    @Override
    public PlanCheckView check(Integer projectId) {
        PlanCheckResult result = runPlanCheckUseCase.run(projectId);
        return new PlanCheckView(
                result.getMilestonesChecked(),
                result.getResults().stream()
                        .map(item -> new PlanCheckItemView(
                                item.getMilestoneId(),
                                item.getTitle(),
                                item.getStatus(),
                                item.getActualState(),
                                item.getError()
                        ))
                        .toList()
        );
    }

    private MilestoneStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MilestoneStatus.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid status. Must be one of: [not_started, on_track, delayed, completed]");
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

    private PlanMilestoneView toMilestoneView(PlanMilestoneResult milestone) {
        return new PlanMilestoneView(
                milestone.getId(),
                milestone.getWeekNumber(),
                milestone.getTitle(),
                milestone.getDescription() == null ? "" : milestone.getDescription(),
                milestone.getExpectedState() == null ? "" : milestone.getExpectedState(),
                milestone.getActualState(),
                milestone.getStatus().toPersistenceValue(),
                milestone.getCheckedAt() == null ? null : milestone.getCheckedAt().toString(),
                milestone.getCreatedAt() == null ? null : milestone.getCreatedAt().toString()
        );
    }
}

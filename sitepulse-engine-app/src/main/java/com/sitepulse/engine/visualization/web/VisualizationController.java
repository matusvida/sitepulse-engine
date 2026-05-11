package com.sitepulse.engine.visualization.web;

import com.sitepulse.engine.http.visualization.api.VisualizationApi;
import com.sitepulse.engine.http.visualization.dto.VisualizeRequest;
import com.sitepulse.engine.http.visualization.dto.VisualizationResultView;
import com.sitepulse.engine.visualization.application.result.VisualizationBatchResult;
import com.sitepulse.engine.visualization.application.usecase.GenerateDetectionVisualizationUseCase;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VisualizationController implements VisualizationApi {

    private final GenerateDetectionVisualizationUseCase generateDetectionVisualizationUseCase;

    @Override
    @PreAuthorize("@projectAccessAuthorizationService.hasProjectAccess(authentication, #projectId)")
    public VisualizationResultView visualize(Integer projectId, VisualizeRequest request) {
        VisualizationBatchResult result = generateDetectionVisualizationUseCase.generate(
                projectId,
                LocalDate.parse(request.getDateFrom()),
                LocalDate.parse(request.getDateTo())
        );
        return new VisualizationResultView(result.getImagesFound(), result.getImagesProcessed(), result.getErrors());
    }
}

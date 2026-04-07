package com.sitepulse.engine.http.visualization.api;

import com.sitepulse.engine.http.visualization.dto.VisualizeRequest;
import com.sitepulse.engine.http.visualization.dto.VisualizationResultView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Visualization")
@RequestMapping("/api")
public interface VisualizationApi {

    @Operation(summary = "Generate a visualization overlay")
    @PostMapping("/projects/{projectId}/visualize")
    VisualizationResultView visualize(@PathVariable Integer projectId, @Valid @RequestBody VisualizeRequest request);
}

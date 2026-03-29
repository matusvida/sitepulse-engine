package com.sitepulse.engine.http.detection.api;

import com.sitepulse.engine.http.detection.dto.DetectRequest;
import com.sitepulse.engine.http.detection.dto.DetectResponse;
import com.sitepulse.engine.http.detection.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Detection")
public interface DetectionApi {

    @Operation(summary = "Get YOLO service health")
    @GetMapping("/health")
    HealthResponse health();

    @Operation(summary = "Run on-demand object detection")
    @PostMapping("/detect")
    DetectResponse detect(@Valid @RequestBody DetectRequest request);
}

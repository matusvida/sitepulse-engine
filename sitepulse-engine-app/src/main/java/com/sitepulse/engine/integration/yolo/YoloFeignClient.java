package com.sitepulse.engine.integration.yolo;

import com.sitepulse.engine.config.FeignConfig;
import com.sitepulse.engine.integration.yolo.dto.YoloHealthResponse;
import com.sitepulse.engine.integration.yolo.dto.YoloInferRequest;
import com.sitepulse.engine.integration.yolo.dto.YoloInferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "yoloClient", url = "${sitepulse.python-yolo-base-url}", configuration = FeignConfig.class)
public interface YoloFeignClient {

    @GetMapping("/health")
    YoloHealthResponse health();

    @PostMapping("/infer")
    YoloInferResponse infer(@RequestBody YoloInferRequest request);
}

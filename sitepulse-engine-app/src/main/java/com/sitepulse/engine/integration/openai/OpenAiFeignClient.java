package com.sitepulse.engine.integration.openai;

import com.sitepulse.engine.config.FeignConfig;
import com.sitepulse.engine.integration.openai.dto.OpenAiChatRequest;
import com.sitepulse.engine.integration.openai.dto.OpenAiChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "openAiClient", url = "https://api.openai.com", configuration = FeignConfig.class)
public interface OpenAiFeignClient {

    @PostMapping(value = "/v1/chat/completions", consumes = "application/json")
    OpenAiChatResponse chat(
            @RequestHeader("Authorization") String authorization,
            @RequestBody OpenAiChatRequest request
    );
}

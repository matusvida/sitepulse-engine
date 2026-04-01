package com.sitepulse.engine.common.infrastructure.external.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.exception.ConfigurationException;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.MilestoneEvaluationPayload;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.OpenAiChatRequest;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.OpenAiChatResponse;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.OpenAiImagePayload;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.ParsedPlanMilestonePayload;
import com.sitepulse.engine.config.SitePulseProperties;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private static final String PLAN_SYSTEM_PROMPT = """
            You are a construction project analyst. You receive the text extracted from
            a construction plan PDF. Your job is to identify the key milestones / phases
            and return them as a JSON array.

            Each milestone object MUST have these fields:
            - "week_number": integer
            - "title": string
            - "description": string
            - "expected_state": string

            Return ONLY a JSON object with field "milestones".
            """;

    private static final String REPORT_SYSTEM_PROMPT = """
            You are a construction progress analyst for SitePulse.
            Write a markdown progress report with:
            1. Executive Summary
            2. Visual Progress
            3. Activity Analysis
            4. Plan Compliance
            5. Risk Assessment
            6. Recommendations
            """;

    private static final String EVAL_SYSTEM_PROMPT = """
            You are a construction milestone evaluator.
            Return ONLY a JSON object with:
            - "status": one of "completed", "on_track", "delayed", "not_started"
            - "actual_state": 1-2 sentence description
            - "confidence": float 0-1
            """;

    private final OpenAiFeignClient openAiFeignClient;
    private final SitePulseProperties properties;
    private final ObjectMapper objectMapper;

    public List<ParsedPlanMilestonePayload> parsePlanMilestones(String pdfText) {
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", PLAN_SYSTEM_PROMPT),
                                Map.of("role", "user", "content", "Here is the construction plan text:\n\n" + truncate(pdfText, 30_000))
                        ),
                        Map.of("type", "json_object"),
                        0.2,
                        4096
                )
        );
        return readMilestones(extractContent(response));
    }

    public String generateProgressReport(List<OpenAiImagePayload> imageData, String metricsContext, String milestonesContext) {
        List<Map<String, Object>> contentParts = new ArrayList<>();
        StringBuilder textBlock = new StringBuilder("Generate a construction progress report.\n\n");
        if (!metricsContext.isBlank()) {
            textBlock.append("## Metrics\n").append(metricsContext).append("\n\n");
        }
        if (!milestonesContext.isBlank()) {
            textBlock.append("## Plan Milestones\n").append(milestonesContext).append("\n\n");
        }
        textBlock.append("## Site Photos\nBelow are site photos from the report period:\n");
        contentParts.add(Map.of("type", "text", "text", textBlock.toString()));
        for (OpenAiImagePayload image : imageData) {
            contentParts.add(Map.of("type", "text", "text", "Photo date: " + image.getDate()));
            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/jpeg;base64," + image.getBase64Content(), "detail", "low")
            ));
        }
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", REPORT_SYSTEM_PROMPT),
                                Map.of("role", "user", "content", contentParts)
                        ),
                        null,
                        0.3,
                        4096
                )
        );
        return extractContent(response);
    }

    public MilestoneEvaluationPayload evaluateMilestone(String title, String expectedState, List<byte[]> images) {
        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(Map.of(
                "type", "text",
                "text", "Milestone: " + title + "\nExpected state: " + expectedState + "\n\nEvaluate the following site photos against this milestone:"
        ));
        for (byte[] image : images.stream().limit(5).toList()) {
            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image), "detail", "low")
            ));
        }
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", EVAL_SYSTEM_PROMPT),
                                Map.of("role", "user", "content", contentParts)
                        ),
                        Map.of("type", "json_object"),
                        0.2,
                        1024
                )
        );
        return readMilestoneEvaluation(extractContent(response));
    }

    private String authorizationHeader() {
        if (properties.openaiApiKey() == null || properties.openaiApiKey().isBlank()) {
            throw new ConfigurationException("OPENAI_API_KEY is not configured");
        }
        return "Bearer " + properties.openaiApiKey();
    }

    private String extractContent(OpenAiChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty() || response.getChoices().getFirst().getMessage() == null) {
            throw new ExternalServiceException("OpenAI returned an empty response");
        }
        return response.getChoices().getFirst().getMessage().getContent();
    }

    private List<ParsedPlanMilestonePayload> readMilestones(String json) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            Object milestones = parsed.getOrDefault("milestones", List.of());
            return objectMapper.convertValue(milestones, new TypeReference<List<ParsedPlanMilestonePayload>>() {});
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new ExternalServiceException("Failed to parse milestone response", ex);
        }
    }

    private MilestoneEvaluationPayload readMilestoneEvaluation(String json) {
        try {
            return objectMapper.readValue(json, MilestoneEvaluationPayload.class);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Failed to parse OpenAI response", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

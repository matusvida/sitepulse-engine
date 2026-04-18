package com.sitepulse.engine.common.infrastructure.external.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.exception.ConfigurationException;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiPrompts.Detection;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiPrompts.Evaluation;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiPrompts.Plan;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiPrompts.Report;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.MilestoneEvaluationPayload;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.ConstructionDetectionPayload;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

    private static final ImageFormat OPENAI_IMAGE_FORMAT = ImageFormat.JPEG;
    private final OpenAiFeignClient openAiFeignClient;
    private final SitePulseProperties properties;
    private final ObjectMapper objectMapper;

    public List<ParsedPlanMilestonePayload> parsePlanMilestones(String pdfText) {
        log.info("OpenAI request type=plan_milestones model={} text_chars={}", properties.openaiModel(), pdfText == null ? 0 : pdfText.length());
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", Plan.SYSTEM_PROMPT),
                                Map.of("role", "user", "content", "Here is the construction plan text:\n\n" + truncate(pdfText, 30_000))
                        ),
                        Map.of("type", "json_object"),
                        0.2,
                        4096
                )
        );
        String content = extractContent(response);
        log.info("OpenAI response type=plan_milestones model={} content_chars={} preview={}", properties.openaiModel(), content.length(), preview(content));
        return readMilestones(content);
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
        textBlock.append("""
                ## Evidence Images
                Below are evidence images from the report period.
                Do not output placeholder image headings such as Photo 1, Photo 2, or Photo 3.
                The frontend will render clickable evidence image links separately.
                Use timestamps only when they support a concrete observation.
                
                """);
        contentParts.add(Map.of("type", "text", "text", textBlock.toString()));
        for (OpenAiImagePayload image : imageData) {
            contentParts.add(Map.of("type", "text", "text", "Evidence timestamp: " + image.getDate()));
            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", OPENAI_IMAGE_FORMAT.dataUriPrefix() + image.getBase64Content(), "detail", "high")
            ));
        }
        log.info("OpenAI request type=progress_report model={} images={} metrics_chars={} milestones_chars={}", properties.openaiModel(), imageData == null ? 0 : imageData.size(), metricsContext == null ? 0 : metricsContext.length(), milestonesContext == null ? 0 : milestonesContext.length());
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", Report.SYSTEM_PROMPT),
                                Map.of("role", "user", "content", contentParts)
                        ),
                        null,
                        0.3,
                        4096
                )
        );
        String content = extractContent(response);
        log.info("OpenAI response type=progress_report model={} content_chars={} preview={}", properties.openaiModel(), content.length(), preview(content));
        return content;
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
                    "image_url", Map.of("url", OPENAI_IMAGE_FORMAT.dataUriPrefix() + Base64.getEncoder().encodeToString(image), "detail", "low")
            ));
        }
        log.info("OpenAI request type=milestone_eval model={} title_chars={} images={}", properties.openaiModel(), title == null ? 0 : title.length(), images == null ? 0 : images.size());
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", Evaluation.SYSTEM_PROMPT),
                                Map.of("role", "user", "content", contentParts)
                        ),
                        Map.of("type", "json_object"),
                        0.2,
                        1024
                )
        );
        String content = extractContent(response);
        log.info("OpenAI response type=milestone_eval model={} content_chars={} preview={}", properties.openaiModel(), content.length(), preview(content));
        return readMilestoneEvaluation(content);
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

    public ConstructionDetectionPayload detectConstructionObjects(byte[] imageBytes, String contextSummary) {
        List<Map<String, Object>> contentParts = new ArrayList<>();
        StringBuilder prompt = new StringBuilder(Detection.LEGACY_DETECTION_USER_PROMPT_TEMPLATE);
        if (contextSummary != null && !contextSummary.isBlank()) {
            prompt.append("\nPrevious tracks:\n").append(contextSummary).append('\n');
        }
        contentParts.add(Map.of("type", "text", "text", prompt.toString()));
        contentParts.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", OPENAI_IMAGE_FORMAT.dataUriPrefix() + Base64.getEncoder().encodeToString(imageBytes), "detail", "low")
        ));
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        List.of(
                                Map.of("role", "system", "content", Detection.SYSTEM_PROMPT),
                                Map.of("role", "user", "content", contentParts)
                        ),
                        Map.of("type", "json_object"),
                        0.0,
                        4096
                )
        );
        return readDetectionPayload(extractContent(response));
    }

    private MilestoneEvaluationPayload readMilestoneEvaluation(String json) {
        try {
            return objectMapper.readValue(json, MilestoneEvaluationPayload.class);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Failed to parse OpenAI response", ex);
        }
    }

    private ConstructionDetectionPayload readDetectionPayload(String json) {
        try {
            return objectMapper.readValue(json, ConstructionDetectionPayload.class);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Failed to parse OpenAI detection response", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 240 ? cleaned : cleaned.substring(0, 240) + "...";
    }
}

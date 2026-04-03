package com.sitepulse.engine.detection.infrastructure.external.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiPrompts.Detection;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiFeignClient;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.OpenAiChatRequest;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.OpenAiChatResponse;
import com.sitepulse.engine.detection.application.service.DetectionClassCatalog;
import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.RawDetection;
import com.sitepulse.engine.detection.infrastructure.external.openai.dto.OpenAiDetectionItem;
import com.sitepulse.engine.detection.infrastructure.external.openai.dto.OpenAiDetectionPayload;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiDetectionGateway {

    private static final String PROMPT_VERSION = "v1";
    private final OpenAiFeignClient openAiFeignClient;
    private final SitePulseProperties properties;
    private final ObjectMapper objectMapper;
    private final DetectionClassCatalog detectionClassCatalog;

    public OpenAiDetectionResult infer(byte[] imageBytes, DetectionContext context, Integer cameraWidth, Integer cameraHeight) {
        BufferedImage image = decode(imageBytes);
        log.info(
                "OpenAI detection request model={} prompt_version={} image={}x{} camera={}x{} context_image_id={} context_items={} classes={}",
                properties.openaiModel(),
                PROMPT_VERSION,
                image.getWidth(),
                image.getHeight(),
                cameraWidth,
                cameraHeight,
                context == null ? null : context.imageId(),
                context == null || context.detections() == null ? 0 : context.detections().size(),
                summarizeClasses()
        );
        long started = System.nanoTime();
        OpenAiChatResponse response = openAiFeignClient.chat(
                authorizationHeader(),
                new OpenAiChatRequest(
                        properties.openaiModel(),
                        buildMessages(imageBytes, context, image.getWidth(), image.getHeight(), cameraWidth, cameraHeight),
                        Map.of("type", "json_object"),
                        0.0,
                        2048
                )
        );
        double latencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
        String json = extractContent(response);
        log.info(
                "OpenAI detection response model={} prompt_version={} latency_ms={} response_chars={} preview={}",
                properties.openaiModel(),
                PROMPT_VERSION,
                latencyMs,
                json.length(),
                preview(json)
        );
        OpenAiDetectionPayload payload = parsePayload(json);
        List<RawDetection> detections = toRawDetections(payload, image.getWidth(), image.getHeight());
        log.debug(
                "OpenAI detection parsed model={} prompt_version={} detections={}",
                properties.openaiModel(),
                PROMPT_VERSION,
                detections.size()
        );
        return new OpenAiDetectionResult(
                new DetectionInference(
                        properties.openaiModel(),
                        image.getWidth(),
                        image.getHeight(),
                        latencyMs,
                        detections
                ),
                json,
                PROMPT_VERSION
        );
    }

    public String promptVersion() {
        return PROMPT_VERSION;
    }

    private List<Map<String, Object>> buildMessages(
            byte[] imageBytes,
            DetectionContext context,
            int imageWidth,
            int imageHeight,
            Integer cameraWidth,
            Integer cameraHeight
    ) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context, imageWidth, imageHeight, cameraWidth, cameraHeight);
        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(Map.of("type", "text", "text", userPrompt));
        contentParts.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes), "detail", "low")
        ));
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", contentParts)
        );
    }

    private String buildSystemPrompt() {
        return Detection.SYSTEM_PROMPT;
    }

    private String buildUserPrompt(DetectionContext context, int imageWidth, int imageHeight, Integer cameraWidth, Integer cameraHeight) {
        String classesJson = serialize(toClassList());
        String contextJson = context == null ? "[]" : serialize(context.detections());
        String contextImageId = context == null ? "none" : String.valueOf(context.imageId());
        String prompt = Detection.USER_PROMPT_TEMPLATE.formatted(classesJson, contextImageId, contextJson);
        if (cameraWidth != null && cameraHeight != null) {
            return prompt + Detection.CAMERA_DIMENSIONS_TEMPLATE.formatted(cameraWidth, cameraHeight);
        }
        return prompt + Detection.FALLBACK_IMAGE_DIMENSIONS_TEMPLATE.formatted(imageWidth, imageHeight);
    }

    private List<Map<String, Object>> toClassList() {
        return detectionClassCatalog.byId().values().stream()
                .sorted(Comparator.comparing(DetectionClassEntity::getId))
                .filter(entry -> entry.getId() != 0)
                .map(entry -> toClassDescriptor(entry.getId(), entry.getClassName()))
                .toList();
    }

    private Map<String, Object> toClassDescriptor(Integer classId, String className) {
        Detection.ClassHint hint = Detection.CLASS_HINTS.get(className);
        if (hint == null) {
            return Map.of(
                    "class_id", classId,
                    "class_name", className
            );
        }
        return Map.of(
                "class_id", classId,
                "class_name", className,
                "typical_width_m", hint.typicalWidthM(),
                "typical_height_m", hint.typicalHeightM(),
                "typical_length_m", hint.typicalLengthM(),
                "detection_hints", hint.detectionHints()
        );
    }

    private List<RawDetection> toRawDetections(OpenAiDetectionPayload payload, int width, int height) {
        if (payload == null || payload.getDetections() == null) {
            return List.of();
        }
        List<RawDetection> raw = new ArrayList<>();
        for (OpenAiDetectionItem item : payload.getDetections()) {
            DetectionClassEntity resolved = resolveDetectionClass(item.getClassId(), item.getClassName());
            List<Double> bbox = normalizeBbox(item.getBboxXyxy(), width, height);
            Double score = normalizeScore(item.getScore());
            String colorHint = normalizeColor(item.getColorHint());
            String notes = normalizeNotes(item.getNotes());
            Integer trackId = resolveTrackId(item);
            raw.add(new RawDetection(
                    resolved.getId(),
                    resolved.getClassName(),
                    score,
                    bbox,
                    trackId,
                    colorHint,
                    notes
            ));
        }
        return raw;
    }

    private DetectionClassEntity resolveDetectionClass(Integer classId, String className) {
        if (classId != null) {
            return detectionClassCatalog.findById(classId)
                    .orElseThrow(() -> new ExternalServiceException("Unknown class_id from OpenAI: " + classId));
        }
        if (className != null && !className.isBlank()) {
            return detectionClassCatalog.findByName(className)
                    .orElseThrow(() -> new ExternalServiceException("Unknown class_name from OpenAI: " + className));
        }
        throw new ExternalServiceException("OpenAI detection missing class_id and class_name");
    }

    private Integer resolveTrackId(OpenAiDetectionItem item) {
        if (item.getSameOrUnique() == null) {
            return null;
        }
        String normalized = item.getSameOrUnique().trim().toLowerCase(Locale.ROOT);
        if (!"same".equals(normalized)) {
            return null;
        }
        return item.getMatchedTrackId();
    }

    private List<Double> normalizeBbox(List<Double> bbox, int width, int height) {
        if (bbox == null || bbox.size() != 4) {
            throw new ExternalServiceException("OpenAI detection missing bbox_xyxy");
        }
        double x1 = clamp(bbox.get(0), 0, width);
        double y1 = clamp(bbox.get(1), 0, height);
        double x2 = clamp(bbox.get(2), 0, width);
        double y2 = clamp(bbox.get(3), 0, height);
        return List.of(x1, y1, x2, y2);
    }

    private Double normalizeScore(Double score) {
        if (score == null) {
            return 0.0;
        }
        if (score < 0.0) {
            return 0.0;
        }
        if (score > 1.0) {
            return 1.0;
        }
        return score;
    }

    private String normalizeColor(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private double clamp(Double value, int min, int max) {
        if (value == null) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private BufferedImage decode(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ExternalServiceException("OpenAI detection could not decode image");
            }
            return image;
        } catch (IOException ex) {
            throw new ExternalServiceException("OpenAI detection could not decode image", ex);
        }
    }

    private OpenAiDetectionPayload parsePayload(String json) {
        try {
            return objectMapper.readValue(json, OpenAiDetectionPayload.class);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Failed to parse OpenAI detection response", ex);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Unable to serialize prompt context", ex);
        }
    }

    private String authorizationHeader() {
        if (properties.openaiApiKey() == null || properties.openaiApiKey().isBlank()) {
            throw new ExternalServiceException("OPENAI_API_KEY is not configured");
        }
        return "Bearer " + properties.openaiApiKey();
    }

    private String extractContent(OpenAiChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty() || response.getChoices().getFirst().getMessage() == null) {
            throw new ExternalServiceException("OpenAI returned an empty response");
        }
        return response.getChoices().getFirst().getMessage().getContent();
    }

    private String summarizeClasses() {
        return toClassList().stream()
                .map(entry -> String.valueOf(entry.get("class_name")))
                .collect(Collectors.joining(","));
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 240 ? cleaned : cleaned.substring(0, 240) + "...";
    }
}

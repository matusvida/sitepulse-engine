package com.sitepulse.engine.detection.infrastructure.external.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiPrompts;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.application.service.DetectionClassCatalog;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectionContext;
import com.sitepulse.engine.detection.domain.model.DetectionContextItem;
import com.sitepulse.engine.detection.infrastructure.persistence.DetectionClassEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiDetectionGatewayTest {

    @Test
    void buildUserPromptGroupsAllowedClassesByFamily() {
        OpenAiDetectionGateway gateway = gatewayWithClasses(
                new DetectionClassEntity(1, "worker", "people"),
                new DetectionClassEntity(2, "truck", "truck"),
                new DetectionClassEntity(3, "excavator", "earthmoving")
        );

        String prompt = gateway.buildUserPrompt(null, null, 1280, 720);

        assertEquals(1, occurrences(prompt, "ALLOWED CLASS GROUPS:"));
        assertTrue(prompt.contains("\"class_group\":\"people\""));
        assertTrue(prompt.contains("\"class_group\":\"truck\""));
        assertTrue(prompt.contains("\"class_group\":\"earthmoving\""));
        assertTrue(prompt.contains("class_group is a family hint only"));
        assertTrue(prompt.contains("\"detection_hints\":\"construction worker, often with PPE or workwear\""));
        assertFalse(prompt.contains("typical_width_m"));
        assertFalse(prompt.contains("typical_height_m"));
        assertFalse(prompt.contains("typical_length_m"));
        assertTrue(prompt.indexOf("\"class_group\":\"people\"") < prompt.indexOf("\"class_group\":\"truck\""));
        assertTrue(prompt.indexOf("\"class_group\":\"truck\"") < prompt.indexOf("\"class_group\":\"earthmoving\""));
    }

    @Test
    void buildUserPromptIncludesPreviousDetectionsWithoutRawResponseAndFallbackImageDimensions() {
        OpenAiDetectionGateway gateway = gatewayWithClasses(new DetectionClassEntity(1, "worker", "people"));
        DetectionContext context = new DetectionContext(
                77,
                List.of(new DetectionContextItem(42, 1, "worker", List.of(10.0, 20.0, 30.0, 40.0), "yellow"))
        );

        String prompt = gateway.buildUserPrompt(context, null, 1920, 1080);

        assertTrue(prompt.contains("PRIOR DETECTIONS SNAPSHOT FROM image_id=77"));
        assertTrue(prompt.contains("\"trackId\":42"));
        assertFalse(prompt.contains("PREVIOUS DETECTION RESPONSE NOTE"));
        assertFalse(prompt.contains("{\"detections\":[{\"track_id\":42,\"class_name\":\"worker\"}]}"));
        assertTrue(prompt.contains("CURRENT IMAGE DIMENSIONS:"));
        assertTrue(prompt.contains("- width=1920"));
        assertTrue(prompt.contains("- height=1080"));
    }

    @Test
    void buildUserPromptIncludesRoiSectionOnlyWhenRoiExists() {
        OpenAiDetectionGateway gateway = gatewayWithClasses(new DetectionClassEntity(1, "worker", "people"));
        CameraRoiSettings withRoi = new CameraRoiSettings(
                List.of(
                        List.of(0.0, 0.0),
                        List.of(100.0, 0.0),
                        List.of(100.0, 100.0),
                        List.of(0.0, 100.0)
                ),
                true,
                1600,
                900
        );

        String promptWithRoi = gateway.buildUserPrompt(null, withRoi, 1600, 900);
        String promptWithoutRoi = gateway.buildUserPrompt(null, new CameraRoiSettings(List.of(), false, 1600, 900), 1600, 900);

        assertTrue(promptWithRoi.contains("ROI SITE-BOUNDARY GUIDANCE:"));
        assertTrue(promptWithRoi.contains("drop_outside: true"));
        assertTrue(promptWithRoi.contains("[[0.0,0.0],[100.0,0.0],[100.0,100.0],[0.0,100.0]]"));
        assertTrue(promptWithRoi.contains("CAMERA IMAGE DIMENSIONS:"));
        assertFalse(promptWithoutRoi.contains("ROI SITE-BOUNDARY GUIDANCE:"));
    }

    @Test
    void detectionPromptsAllowPartiallyVisibleVehiclesWhenVisuallySupported() {
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "Partially visible or occluded vehicles and machines may still be valid detections"
        ));
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "Partial occlusion, truncation at the image edge, or hiding behind walls/barriers does not disqualify a real object by itself"
        ));
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "The appearance of a large truck, excavator, or other prominent object must not cause other clearly visible valid objects to disappear from the output"
        ));
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "Use a full-frame process, not a salience-first process"
        ));
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "Secondary signals are cars, vans, pickups, and other light vehicles"
        ));
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "A crane under construction is still a valid crane detection"
        ));
        assertTrue(OpenAiPrompts.Detection.SYSTEM_PROMPT.contains(
                "Do not treat top-of-frame objects as outside the site by default"
        ));

        OpenAiDetectionGateway gateway = gatewayWithClasses(
                new DetectionClassEntity(8, "truck", "truck"),
                new DetectionClassEntity(9, "van", "light_vehicle"),
                new DetectionClassEntity(10, "crane_tower", "lifting")
        );
        String prompt = gateway.buildUserPrompt(null, null, 1920, 1080);

        assertTrue(prompt.contains(
                "A detection may still be valid when only part of a vehicle or truck is visible"
        ));
        assertTrue(prompt.contains(
                "If a van, car, pickup, truck, or machine is clearly visible and clearly inside the monitored site, keep it in the output even when another more prominent object appears"
        ));
        assertTrue(prompt.contains(
                "Prioritize primary construction signals over secondary light-vehicle signals when judging borderline or ambiguous evidence"
        ));
        assertTrue(prompt.contains(
                "Before producing the final JSON, re-scan the parking row / parked-vehicle area"
        ));
        assertTrue(prompt.contains(
                "If a crane is visibly being assembled, erected, or partially built on site, detect it as the appropriate crane class and state in notes that it is under construction"
        ));
        assertTrue(prompt.contains(
                "Do not reject an object merely because it appears small, distant, in the upper part of the image, or stationary"
        ));
        assertTrue(prompt.contains("\"class_group\":\"truck\""));
        assertTrue(prompt.contains("\"class_group\":\"light_vehicle\""));
        assertTrue(prompt.contains("\"class_group\":\"lifting\""));
        assertTrue(prompt.contains(
                "Do not drop a clearly visible parked van, car, pickup, or truck just because a dump truck or other more salient machine is also present"
        ));
    }

    private OpenAiDetectionGateway gatewayWithClasses(DetectionClassEntity... classes) {
        DetectionClassCatalog detectionClassCatalog = new DetectionClassCatalog(null) {
            @Override
            public Map<Integer, DetectionClassEntity> byId() {
                return java.util.Arrays.stream(classes)
                        .collect(java.util.stream.Collectors.toMap(DetectionClassEntity::getId, val -> val));
            }

            @Override
            public Map<String, List<DetectionClassEntity>> byGroup() {
                return java.util.Arrays.stream(classes)
                        .collect(java.util.stream.Collectors.groupingBy(
                                DetectionClassEntity::getClassGroup,
                                java.util.LinkedHashMap::new,
                                java.util.stream.Collectors.toList()
                        ));
            }
        };
        return new OpenAiDetectionGateway(null, testProperties(), new ObjectMapper(), detectionClassCatalog);
    }

    private int occurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static SitePulseProperties testProperties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "tower-tl",
                60,
                new SitePulseProperties.StorageProperties(
                        "http://minio:9000",
                        "http://localhost:9001",
                        "admin",
                        "password123",
                        "us-east-1"
                ),
                "yolov8x.pt",
                0.35,
                "{}",
                25.0,
                false,
                "roi_config.json",
                0.0,
                0,
                255,
                false,
                "0 0/10 * * * *",
                "0 5/10 * * * *",
                "openai",
                "0 0 2 * * *",
                3,
                "",
                "",
                "",
                "",
                "test-key",
                "gpt-4.1",
                52_428_800L,
                "http://python-yolo:8000",
                new SitePulseProperties.ImageWebSnapshotsProperties(false, 1920, 75, ImageFormat.WEBP, java.time.LocalTime.of(17, 0))
        );
    }
}

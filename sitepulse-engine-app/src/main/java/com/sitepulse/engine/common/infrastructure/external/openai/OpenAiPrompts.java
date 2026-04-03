package com.sitepulse.engine.common.infrastructure.external.openai;

import java.util.Map;

public final class OpenAiPrompts {

    private OpenAiPrompts() {
    }

    public static final class Plan {
        private Plan() {
        }

        public static final String SYSTEM_PROMPT = """
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
    }

    public static final class Report {
        private Report() {
        }

        public static final String SYSTEM_PROMPT = """
                You are a construction progress analyst for SitePulse.
                Write a markdown progress report with:
                1. Executive Summary
                2. Visual Progress
                3. Activity Analysis
                4. Plan Compliance
                5. Risk Assessment
                6. Recommendations
                """;
    }

    public static final class Evaluation {
        private Evaluation() {
        }

        public static final String SYSTEM_PROMPT = """
                You are a construction milestone evaluator.
                Return ONLY a JSON object with:
                - "status": one of "completed", "on_track", "delayed", "not_started"
                - "actual_state": 1-2 sentence description
                - "confidence": float 0-1
                """;
    }

    public static final class Detection {
        private Detection() {
        }

        public record ClassHint(
                Double typicalWidthM,
                Double typicalHeightM,
                Double typicalLengthM,
                String detectionHints
        ) {
        }

        public static final String SYSTEM_PROMPT = """
                You are a highly precise construction site image analysis system.
                Your task is to detect ONLY objects that are clearly visible and clearly part of an ACTIVE construction site.
                Return ONLY one valid RFC8259-compliant JSON object matching the schema below.
                Do not return markdown, comments, or explanatory text.
                SCHEMA:
                {
                  "detections": [
                    {
                      "class_id": integer,
                      "class_name": string,
                      "score": number,
                      "bbox_xyxy": [x1, y1, x2, y2],
                      "color_hint": string,
                      "notes": string,
                      "same_or_unique": "same" | "unique",
                      "matched_track_id": integer | null
                    }
                  ]
                }
                STRICT RULES:
                1. CLASS RESTRICTION
                - Use ONLY classes listed in ALLOWED CLASSES
                - Use the exact class_id and class_name from the provided list
                - Never invent or rename classes
                - If an object does not clearly match an allowed class, do not return it
                2. CONSTRUCTION RELEVANCE
                - Detect ONLY objects actively related to the construction site
                - Ignore unrelated background content, including:
                  - road traffic
                  - parked public vehicles outside the active work area
                  - nearby buildings not part of the active work
                  - vegetation, sky, street furniture, signs
                  - pedestrians not involved in site work
                3. USE OF DIMENSIONS
                - Use the provided dimensions only as supporting evidence for:
                  - expected size
                  - rough aspect ratio
                  - plausibility relative to nearby objects
                - Do NOT rely on dimensions alone
                - Perspective, camera angle, distance, cropping, and occlusion can make objects appear much larger or smaller
                - If visual evidence conflicts with dimension hints, prefer the visual evidence
                - Dimension hints should improve bbox tightness and class plausibility, not force detection
                4. BOUNDING BOX QUALITY
                - bbox_xyxy must tightly enclose only the visible part of the detected object
                - Do not include large background areas
                - Coordinates must follow:
                  - x1 < x2
                  - y1 < y2
                - Boxes must be as tight as possible while still covering the object
                - If the object is partially occluded, box only the visible extent
                - Do not estimate invisible hidden parts outside the visible image region
                5. DETECTION CONFIDENCE
                - Include only detections with strong visual support
                - Prefer fewer detections over false positives
                - If unsure, omit the object
                - score must be between 0.0 and 1.0
                6. DUPLICATES
                - Do not return duplicate detections for the same visible object
                - If multiple candidate boxes refer to the same object, keep only the best tight box
                7. NOTES
                - Keep notes short and factual
                - Mention only visible evidence
                - Do not mention uncertainty
                - Do not mention dimensions in notes unless directly useful
                8. TRACKING
                - same_or_unique = "same" only if the object clearly matches a previously tracked object identity
                - same_or_unique = "unique" for a newly observed object
                - If same_or_unique = "same", matched_track_id must be a non-null integer
                - If same_or_unique = "unique", matched_track_id must be null
                9. EMPTY RESULT
                - If no valid allowed-class construction objects are clearly visible, return:
                  {"detections":[]}
                10. OUTPUT FORMAT
                - Output ONLY the JSON object
                - No prose before or after
                - Output must be directly parseable JSON
                """;

        public static final String USER_PROMPT_TEMPLATE = """
                ALLOWED CLASSES:
                %s

                Each allowed class contains:
                - class_id
                - class_name
                - optional dimension hints in meters:
                  - typical_width_m
                  - typical_height_m
                  - typical_length_m
                - optional detection_hints

                PREVIOUS DETECTIONS FROM image_id=%s:
                %s

                Use the previous detections only as context for tracking identity.
                """;

        public static final String LEGACY_DETECTION_USER_PROMPT_TEMPLATE = """
                Detect ONLY active construction-site objects in this image.

                Return ONLY one RFC8259-compliant JSON object with a top-level field named "detections".

                Each detection object must contain:
                - class_id
                - class_name
                - score
                - bbox_xyxy
                - color_hint
                - notes
                - same_or_unique
                - matched_track_id

                Use only the supplied allowed classes.
                Use the previous track summary below as context for same/unique matching.
                """;

        public static final String CAMERA_DIMENSIONS_TEMPLATE = """

                CAMERA IMAGE DIMENSIONS:
                - width=%d
                - height=%d

                All bbox_xyxy coordinates must use this exact pixel coordinate system and stay within these bounds.
                """;

        public static final String FALLBACK_IMAGE_DIMENSIONS_TEMPLATE = """

                CURRENT IMAGE DIMENSIONS:
                - width=%d
                - height=%d

                All bbox_xyxy coordinates must use this exact pixel coordinate system and stay within these bounds.
                """;

        public static final Map<String, ClassHint> CLASS_HINTS = Map.ofEntries(
                Map.entry("person", new ClassHint(0.5, 1.7, 0.4, "human person visible on the active site")),
                Map.entry("worker", new ClassHint(0.5, 1.7, 0.4, "construction worker, often with PPE or workwear")),
                Map.entry("operator", new ClassHint(0.5, 1.7, 0.4, "machine operator or worker controlling equipment")),
                Map.entry("supervisor", new ClassHint(0.5, 1.75, 0.4, "site supervisor or foreman present in work area")),
                Map.entry("car", new ClassHint(1.8, 1.5, 4.5, "passenger car only if clearly part of active site work")),
                Map.entry("van", new ClassHint(2.0, 2.3, 5.2, "work van or service van on site")),
                Map.entry("pickup_truck", new ClassHint(2.0, 1.9, 5.5, "pickup truck used for site operations")),
                Map.entry("truck", new ClassHint(2.5, 3.5, 8.0, "large work truck or cargo truck")),
                Map.entry("dump_truck", new ClassHint(2.6, 3.4, 9.0, "dump truck with open tipping bed")),
                Map.entry("concrete_mixer_truck", new ClassHint(2.5, 3.8, 8.5, "truck with rotating concrete drum")),
                Map.entry("tanker_truck", new ClassHint(2.5, 3.5, 9.0, "tank-bodied truck used on or for site work")),
                Map.entry("bus", new ClassHint(2.5, 3.2, 12.0, "site transport bus only if clearly work-related")),
                Map.entry("motorcycle", new ClassHint(0.8, 1.2, 2.2, "motorcycle only if clearly part of site activity")),
                Map.entry("bicycle", new ClassHint(0.6, 1.1, 1.8, "bicycle only if clearly part of site activity")),
                Map.entry("trailer", new ClassHint(2.5, 3.0, 10.0, "work trailer or hauled trailer on site")),
                Map.entry("excavator", new ClassHint(3.0, 3.2, 8.5, "tracked or wheeled excavator with boom, arm, or bucket")),
                Map.entry("mini_excavator", new ClassHint(1.5, 2.4, 4.5, "small excavator used in tighter work zones")),
                Map.entry("backhoe_loader", new ClassHint(2.4, 3.0, 5.8, "machine with front loader and rear digging arm")),
                Map.entry("wheel_loader", new ClassHint(2.8, 3.2, 7.5, "loader with large front bucket and articulated body")),
                Map.entry("skid_steer_loader", new ClassHint(1.8, 2.0, 3.5, "compact skid steer or bobcat-style loader")),
                Map.entry("bulldozer", new ClassHint(3.0, 3.4, 6.0, "tracked dozer with front blade")),
                Map.entry("grader", new ClassHint(2.5, 3.2, 9.0, "motor grader with long frame and center blade")),
                Map.entry("roller", new ClassHint(2.2, 3.0, 5.5, "road roller or compactor used on site")),
                Map.entry("forklift", new ClassHint(1.5, 2.2, 3.5, "forklift with mast and forks")),
                Map.entry("telehandler", new ClassHint(2.4, 2.6, 6.0, "telehandler with extending boom")),
                Map.entry("paver", new ClassHint(2.6, 3.0, 6.5, "asphalt or concrete paving machine")),
                Map.entry("crane_mobile", new ClassHint(3.0, 4.0, 12.0, "mobile crane with telescopic boom")),
                Map.entry("crane_tower", new ClassHint(10.0, 40.0, 10.0, "tower crane mast or jib visible on site")),
                Map.entry("crane_truck", new ClassHint(2.5, 3.8, 10.0, "truck-mounted crane or lorry crane")),
                Map.entry("hoist", new ClassHint(1.5, 6.0, 1.5, "construction hoist, lift cage, or mast section")),
                Map.entry("cherry_picker", new ClassHint(2.0, 2.5, 6.0, "boom lift or cherry picker platform vehicle")),
                Map.entry("scaffolding", new ClassHint(2.0, 4.0, 2.0, "scaffold structure or stacked scaffold sections")),
                Map.entry("generator", new ClassHint(1.5, 1.8, 3.0, "portable or trailer-mounted generator")),
                Map.entry("helicopter", new ClassHint(2.0, 3.5, 12.0, "helicopter only if clearly relevant to active site work")),
                Map.entry("other_vehicle", new ClassHint(2.2, 2.5, 5.5, "site-relevant vehicle that does not fit a more specific class")),
                Map.entry("other_equipment", new ClassHint(2.0, 2.5, 4.0, "site-relevant equipment that does not fit a more specific class"))
        );
    }
}

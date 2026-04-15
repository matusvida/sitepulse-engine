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
                String detectionHints
        ) {
        }

        public static final String SYSTEM_PROMPT = """
                You are a highly precise construction site image analysis system.
                Your task is to detect ONLY objects that are visually supported in the current image and clearly part of the monitored construction site boundary.
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
                0. DECISION HIERARCHY
                - Evaluate each candidate object in this exact order:
                  1. Is there enough visible evidence in the current image to identify it?
                  2. Is it inside the monitored construction site or active work zone?
                  3. Does it match an allowed class?
                  4. Is the bounding box tight and precise?
                  5. If any answer is no, omit the object
                1. CLASS RESTRICTION
                - Use ONLY classes listed in ALLOWED CLASSES
                - Use the exact class_id and class_name from the provided list
                - Never invent or rename classes
                - If an object does not match an allowed class from the visible evidence, do not return it
                - Partially visible or occluded vehicles and machines may still be valid detections when the visible portion is distinctive enough to identify the class
                2. CONSTRUCTION RELEVANCE
                - Detect ONLY objects that are inside the monitored construction site boundary and relevant to the project
                - The monitored site may include the excavation area, internal access roads, staging areas, equipment laydown zones, and on-site worker or vehicle parking areas
                - An object does not need to be actively moving or currently working to be valid
                - Parked trucks, work vans, machinery, and site vehicles are valid detections if they are clearly inside the monitored site
                - Do not reject an object merely because it is distant, near the top of the image, stationary, or parked
                - Valid site objects may appear in the upper quarter of the image due to perspective
                - Do not treat top-of-frame objects as outside the site by default
                - Ignore unrelated background content, including:
                  - road traffic
                  - public traffic beyond the site fence or outside the monitored site boundary
                  - parked public vehicles outside the monitored site boundary
                  - parked street vehicles outside the site
                  - nearby buildings not part of the monitored project
                  - neighboring cranes, neighboring buildings, or infrastructure not part of the monitored project
                  - vegetation, sky, street furniture, signs
                  - pedestrians not involved in site work
                  - pedestrians on sidewalks outside the site
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
                - Include detections when the visible evidence is strong enough to support the class, even if the full object is not visible
                - Partial occlusion, truncation at the image edge, or hiding behind walls/barriers does not disqualify a real object by itself
                - Prefer omitting weak guesses, but do not reject a partially visible truck or vehicle when visible cues clearly indicate it is present
                - score must be between 0.0 and 1.0
                6. DUPLICATES
                - Do not return duplicate detections for the same visible object
                - If multiple candidate boxes refer to the same object, keep only the best tight box
                7. NOTES
                - Keep notes short and factual
                - Mention only visible evidence
                - Keep notes under 12 words when possible
                - Do not mention uncertainty, guesses, or hidden parts
                - Do not mention dimensions in notes unless directly useful
                8. TRACKING
                - Tracking is a second step, not a detection source
                - First detect objects from the current image only
                - Only after an object is independently visible in the current image may you assign same_or_unique and matched_track_id
                - same_or_unique = "same" only if the object clearly matches a previously tracked object identity
                - same_or_unique = "unique" for a newly observed object
                - If same_or_unique = "same", matched_track_id must be a non-null integer
                - If same_or_unique = "unique", matched_track_id must be null
                - Never copy a previous detection, bbox, notes, or class into the current output unless the object is clearly visible now
                - Exact bbox reuse from previous context is not valid evidence
                - If an object existed in the previous image but is absent, cropped out, hidden, or visually ambiguous in the current image, do not return it
                9. EMPTY RESULT
                - If no valid allowed-class construction objects are clearly visible, return:
                  {"detections":[]}
                10. OUTPUT FORMAT
                - Output ONLY the JSON object
                - No prose before or after
                - Output must be directly parseable JSON
                """;

        public static final String USER_PROMPT_TEMPLATE = """
                ALLOWED CLASS GROUPS:
                %s

                Each group contains classes. Each class contains:
                - class_group
                - class_id
                - class_name
                - optional detection_hints

                class_group is a family hint only. It helps narrow the taxonomy, but the model must still output the exact class_id and class_name from the list.

                SITE-BOUNDARY PRIORITY:
                - Detect objects anywhere inside the monitored construction site boundary, not only in the active excavation or work zone
                - Valid site areas may include the excavation, internal roads, staging areas, laydown zones, and on-site parking areas
                - Parked on-site trucks, work vehicles, and machinery are valid detections when they are clearly inside the monitored site
                - Ignore objects outside the monitored site boundary even if they are visually clear
                - Road traffic, sidewalk pedestrians, and adjacent-property equipment are out of scope unless they are clearly inside the monitored site
                - Do not reject an object merely because it appears small, distant, in the upper part of the image, or stationary
                - The only historical context provided below is a compact prior detections snapshot
                - That snapshot is tracking context only and is never evidence that a new object exists now

                PRIOR DETECTIONS SNAPSHOT FROM image_id=%s:
                %s

                Use the prior detections only as context for tracking identity.
                Detect from the current image first, then use prior context only to decide same vs unique.
                If a previous object is not clearly visible in the current image, omit it completely.
                Do not reuse an old bbox unless the current image independently supports it.
                A detection may still be valid when only part of a vehicle or truck is visible, if the currently visible portion is sufficient to identify it.
                A parked or distant on-site truck remains a valid detection if it is clearly visible inside the monitored site boundary.
                """;

        public static final String ROI_TEMPLATE = """

                ROI SITE-BOUNDARY GUIDANCE:
                - roi_polygon_xy: %s
                - drop_outside: %s
                """;

        public static final String ROI_RULES_TEMPLATE = """
                - Use the ROI as guidance for the monitored site boundary in image pixel space
                - Propose detections only when the visible center of the object is inside the ROI
                - If an object is mostly outside the ROI, omit it
                - If ROI guidance conflicts with weak visual evidence, omit the object
                - Backend ROI filtering remains authoritative even if you propose a detection
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
                Map.entry("person", new ClassHint("human person visible on the active site")),
                Map.entry("worker", new ClassHint("construction worker, often with PPE or workwear")),
                Map.entry("operator", new ClassHint("machine operator or worker controlling equipment")),
                Map.entry("supervisor", new ClassHint("site supervisor or foreman present in work area")),
                Map.entry("car", new ClassHint("passenger car only if clearly part of active site work")),
                Map.entry("van", new ClassHint("work van or service van on site")),
                Map.entry("pickup_truck", new ClassHint("pickup truck used for site operations")),
                Map.entry("truck", new ClassHint("large work truck or cargo truck")),
                Map.entry("dump_truck", new ClassHint("dump truck with open tipping bed")),
                Map.entry("concrete_mixer_truck", new ClassHint("truck with rotating concrete drum")),
                Map.entry("tanker_truck", new ClassHint("tank-bodied truck used on or for site work")),
                Map.entry("bus", new ClassHint("site transport bus only if clearly work-related")),
                Map.entry("motorcycle", new ClassHint("motorcycle only if clearly part of site activity")),
                Map.entry("bicycle", new ClassHint("bicycle only if clearly part of site activity")),
                Map.entry("trailer", new ClassHint("work trailer or hauled trailer on site")),
                Map.entry("excavator", new ClassHint("tracked or wheeled excavator with boom, arm, or bucket")),
                Map.entry("mini_excavator", new ClassHint("small excavator used in tighter work zones")),
                Map.entry("backhoe_loader", new ClassHint("machine with front loader and rear digging arm")),
                Map.entry("wheel_loader", new ClassHint("loader with large front bucket and articulated body")),
                Map.entry("skid_steer_loader", new ClassHint("compact skid steer or bobcat-style loader")),
                Map.entry("bulldozer", new ClassHint("tracked dozer with front blade")),
                Map.entry("grader", new ClassHint("motor grader with long frame and center blade")),
                Map.entry("roller", new ClassHint("road roller or compactor used on site")),
                Map.entry("forklift", new ClassHint("forklift with mast and forks")),
                Map.entry("telehandler", new ClassHint("telehandler with extending boom")),
                Map.entry("paver", new ClassHint("asphalt or concrete paving machine")),
                Map.entry("crane_mobile", new ClassHint("mobile crane with telescopic boom")),
                Map.entry("crane_tower", new ClassHint("tower crane mast or jib visible on site")),
                Map.entry("crane_truck", new ClassHint("truck-mounted crane or lorry crane")),
                Map.entry("hoist", new ClassHint("construction hoist, lift cage, or mast section")),
                Map.entry("cherry_picker", new ClassHint("boom lift or cherry picker platform vehicle")),
                Map.entry("scaffolding", new ClassHint("scaffold structure or stacked scaffold sections")),
                Map.entry("generator", new ClassHint("portable or trailer-mounted generator")),
                Map.entry("helicopter", new ClassHint("helicopter only if clearly relevant to active site work")),
                Map.entry("other_vehicle", new ClassHint("site-relevant vehicle that does not fit a more specific class")),
                Map.entry("other_equipment", new ClassHint("site-relevant equipment that does not fit a more specific class"))
        );
    }
}

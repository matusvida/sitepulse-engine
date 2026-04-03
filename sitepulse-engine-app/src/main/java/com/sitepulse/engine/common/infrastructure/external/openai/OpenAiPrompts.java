package com.sitepulse.engine.common.infrastructure.external.openai;

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

        public static final String SYSTEM_PROMPT = """
                You are a construction site image analyst.
                Your task is to detect and label construction-relevant objects only.
                Ignore vehicles on roads or parking areas unrelated to the active site.
                Return ONLY a JSON object matching the schema below.

                Schema:
                {
                  "detections": [
                    {
                      "class_id": integer,
                      "class_name": string,
                      "score": number 0..1,
                      "bbox_xyxy": [x1,y1,x2,y2],
                      "color_hint": string,
                      "notes": string,
                      "same_or_unique": "same" | "unique",
                      "matched_track_id": integer | null
                    }
                  ]
                }

                Rules:
                - Output only the JSON object, no prose.
                - Every detection MUST include bbox_xyxy with 4 numbers.
                - Use ONLY the provided class list and color vocabulary.
                - If unsure, return fewer detections rather than guessing.
                - If same_or_unique is "same", matched_track_id MUST be present.
                - If no objects are found, return {"detections": []}.
                """;

        public static final String USER_PROMPT_TEMPLATE = """
                Allowed classes (JSON array of {id, class_name}):
                %s

                Allowed color_hint values (JSON array):
                %s

                Previous detections from image_id=%s (JSON array):
                %s

                For each detected object in the current image, decide if it matches a previous detection.
                If it matches, set same_or_unique="same" and matched_track_id to the prior track_id.
                If it does not match, set same_or_unique="unique" and matched_track_id=null.

                Notes should be short (max 80 chars) and objective.
                """;

        public static final String LEGACY_DETECTION_USER_PROMPT_TEMPLATE = """
                Detect construction-site objects in this image.

                Return JSON with a top-level field named "detections".

                Each detection object must contain:
                - class_id
                - class_name
                - score
                - bbox_xyxy
                - color_hint
                - notes
                - same_or_unique
                - matched_track_id

                Use the previous track summary below as context for same/unique matching.
                """;
    }
}

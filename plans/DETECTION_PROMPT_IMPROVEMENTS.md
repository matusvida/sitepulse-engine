# Detection Prompt Improvements

## Goal

Improve OpenAI-based construction object detection so it is more precise, more ROI-aware, and less likely to return objects outside the active construction site boundary.

The target state is:

- stronger detection prompts aligned with the current parser and post-processing pipeline
- optional ROI context included in the prompt
- deterministic backend ROI enforcement kept as the source of truth
- better observability for prompt-versioned detection behavior

## Current State

The current implementation already has a usable baseline:

- OpenAI detection uses strict JSON output and low-temperature inference
- allowed classes are injected from the detection taxonomy
- previous detections are provided for track continuity
- image dimensions are included for pixel-space bounding boxes
- backend post-processing filters by confidence, area, and ROI
- ROI filtering can already drop detections outside configured camera polygons

Relevant code:

- `sitepulse-engine-app/src/main/java/com/sitepulse/engine/common/infrastructure/external/openai/OpenAiPrompts.java`
- `sitepulse-engine-app/src/main/java/com/sitepulse/engine/detection/infrastructure/external/openai/OpenAiDetectionGateway.java`
- `sitepulse-engine-app/src/main/java/com/sitepulse/engine/detection/domain/service/DetectionPostProcessor.java`
- `sitepulse-engine-app/src/main/java/com/sitepulse/engine/config/SitePulseProperties.java`

## Problems To Address

1. The prompt still relies heavily on natural-language judgment for what counts as part of the active construction site.
2. ROI exists in backend filtering, but the model is not explicitly guided by ROI in the prompt.
3. Objects near roads, sidewalks, neighboring parcels, or background infrastructure can still be proposed by the model before post-filtering.
4. Prompt versioning is present, but prompt changes are not yet structured as a tracked improvement program.
5. The current prompt is strong on format control, but weaker on site-boundary exclusion and decision hierarchy.

## Design Principles

1. Keep backend ROI filtering as the hard enforcement layer.
2. Use the prompt to reduce bad proposals before they reach post-processing.
3. Preserve compatibility with the current JSON schema unless there is a strong reason to change it.
4. Prefer precision over recall.
5. Make prompt changes measurable through prompt versioning and run analysis.

## Implementation Plan

### Phase 1: Strengthen The Prompt Contract

Update `OpenAiPrompts.Detection.SYSTEM_PROMPT` and `USER_PROMPT_TEMPLATE` to:

- explicitly prioritize site-boundary exclusion
- state that the model must ignore objects outside the active work zone
- clarify that road traffic and adjacent-property objects are out of scope even if visually clear
- reinforce that previous detections are tracking context only, not evidence for new detections
- require tighter notes discipline and stronger rejection of ambiguous objects

Specific prompt additions:

- Add a decision hierarchy:
  1. Is the object clearly visible?
  2. Is it inside the construction site or active work zone?
  3. Does it match an allowed class?
  4. Is the box tight and precise?
  5. If not all are true, omit it.
- Add explicit exclusion examples:
  - public traffic beyond the site fence
  - parked street vehicles outside the site
  - pedestrians on sidewalks outside the site
  - neighboring cranes/buildings not part of the monitored project

Deliverable:

- revised prompt text in `OpenAiPrompts.java`

### Phase 2: Inject ROI Context Into The Prompt

Extend the OpenAI user prompt so it can optionally include per-camera ROI information when available.

Implementation steps:

1. Add ROI prompt fragments to `OpenAiPrompts.Detection`, for example:
   - `ROI_TEMPLATE`
   - `ROI_RULES_TEMPLATE`
2. Pass ROI data into prompt construction from `OpenAiDetectionGateway`.
3. Format ROI as a compact JSON polygon or ordered point list in image pixel coordinates.
4. Instruct the model:
   - detections should be proposed only for objects whose visible center is inside the ROI
   - if the object is mostly outside ROI, omit it
   - ROI is guidance for site bounds, but backend enforcement remains authoritative

Suggested code changes:

- extend `buildUserPrompt(...)` in `OpenAiDetectionGateway`
- add ROI-aware prompt assembly helpers
- source ROI polygon from the same per-camera settings already used by `DetectionPostProcessor`

Deliverable:

- ROI-aware prompt generation path with graceful fallback when no ROI exists

### Phase 3: Unify ROI Data Flow

Ensure the same ROI definition reaches both:

- prompt generation for model guidance
- post-processing for deterministic filtering

Implementation steps:

1. Trace the current camera ROI settings flow into detection execution.
2. If ROI is only available in post-processing, expose it earlier in the detection request path.
3. Add a small DTO or model object if needed to carry:
   - `roiPolygon`
   - `dropOutside`
4. Keep one canonical ROI coordinate system in image pixels.

Deliverable:

- a single consistent ROI source used throughout the detection pipeline

### Phase 4: Improve Prompt Versioning And Observability

The gateway already stores `promptVersion`. Expand that into a deliberate rollout mechanism.

Implementation steps:

1. Change `PROMPT_VERSION` from a generic value like `v1` to a meaningful version such as:
   - `v2-roi-guided`
2. Log whether ROI was included in the prompt.
3. Record detection counts and failure reasons per prompt version.
4. Keep raw responses for comparison during rollout.

Deliverable:

- prompt-versioned analysis of quality before and after ROI-aware prompt changes

### Phase 5: Add Focused Tests

Add tests around prompt construction and ROI behavior.

Recommended coverage:

- prompt includes allowed classes exactly once
- prompt includes previous detections when context exists
- prompt includes image dimensions
- prompt includes ROI polygon only when ROI exists
- prompt omits ROI section when ROI is unavailable
- ROI post-filter still drops detections outside polygon when `dropOutside=true`

Potential test targets:

- `OpenAiDetectionGateway`
- `DetectionPostProcessor`
- any new prompt-builder helper introduced during refactor

Deliverable:

- automated regression coverage for prompt composition and ROI rules

### Phase 6: Production Rollout

Roll out safely and measure precision improvements.

Steps:

1. Deploy revised prompt with new prompt version.
2. Compare runs on representative camera views:
   - open site
   - fenced site near roads
   - dense urban project
   - partial occlusion scenes
   - low-light scenes
3. Review:
   - false positives outside site bounds
   - duplicate objects
   - track continuity stability
   - detections dropped by ROI post-filter
4. Tune prompt wording only after reviewing real outputs.

Deliverable:

- validated production prompt behavior with reduced out-of-site detections

## Recommended Sequence

1. Strengthen prompt wording without changing schema.
2. Add ROI prompt context.
3. Unify ROI propagation through the detection pipeline.
4. Add tests.
5. Roll out with new prompt version and review real detections.

## Non-Goals

- replacing backend ROI filtering with prompt-only logic
- changing the detection JSON schema unless required
- adding complex geometric overlap scoring before the prompt improvement is evaluated
- broadening the Python YOLO service

## Risks

- Over-constraining the prompt may reduce recall too much.
- ROI guidance in the prompt may still be inconsistently followed by the model.
- If ROI coordinates do not match the actual analyzed image dimensions, prompt guidance could become misleading.
- Prompt changes without test coverage may silently break parsing expectations.

## Success Criteria

- fewer false positives outside construction site boundaries
- no regression in JSON parse reliability
- no increase in unknown class failures
- tighter alignment between model proposals and backend ROI enforcement
- prompt-versioned runs show measurable precision improvement on representative project images

## Optional Future Improvements

- add a second-stage validator that rejects detections outside ROI before persistence
- store whether each detection was inside ROI at inference-time versus post-processing-time
- add camera-specific textual site descriptions to prompt context
- support polygon-overlap-based filtering instead of bbox-center-only ROI logic if needed

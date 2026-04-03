# OpenAI-First Detection MVP With YOLO Fallback

## Summary
- Replace the current YOLO-first detection path with an OpenAI-first image recognition pipeline for construction-site objects.
- Keep YOLO as the guaranteed fallback only when OpenAI is configured as the primary provider and the OpenAI attempts fail.
- Preserve the existing `image_id -> detections` relationship, but add the schema needed for stable tracking across consecutive frames.

## Data Model
- Add a static `detection_classes` table with `id` and `class_name`, seeded by Flyway.
- Seed a focused construction taxonomy covering people, vehicles, and equipment, including vehicles, cranes, excavators, loaders, forklifts, telehandlers, rollers, graders, pavers, scaffolding, hoists, generators, and `helicopter` as a rare but valid class.
- Extend `detections` with `color_hint` and `notes`.
- Keep `model_version`, `score`, and `bbox_xyxy` on `detections`; `bbox_xyxy` remains required and must always be returned by OpenAI.
- Add a new `detection_tracks` table to represent a stable object across images, keyed by project and camera scope, with first-seen / last-seen image references, active state, current class, current box, and current color hint.
- Add a new `detection_analysis_runs` table to record every detection attempt, including provider, model version, prompt version, retry count, image id, status, latency, error, and raw JSON response.
- Add `track_id` and `analysis_run_id` to `detections` so each detection row is linked to both the stable object and the exact model call that produced it.

## Detection Flow
- Refactor `ProcessPendingImagesUseCase` so it delegates to a detection strategy instead of calling the current gateway directly.
- Keep the pipeline anchored to `detection-sweep-cron`; the scheduled job still claims pending images and processes them one by one.
- For each image, the strategy should:
  - load the current image bytes
  - load the previous normalized detections from the database for the same project/camera scope
  - build a compact context summary from those rows
  - call the primary provider
  - validate and persist the OpenAI response
  - update tracks
- For the MVP, do not send the previous image itself to OpenAI; send only the current image plus the normalized summary from Postgres.
- Keep the prompt input compact by including only the last few active tracks rather than every historical detection for the day.
- If OpenAI fails or returns invalid output, retry twice with the same input.
- On the third failure, call YOLO and persist the YOLO result so the pipeline still completes.
- Support a config-driven primary provider:
  - `openai` means OpenAI first, then YOLO fallback
  - `yolo` means YOLO only

## OpenAI Contract
- Add a dedicated OpenAI detection service and DTO set, separate from report and plan generation.
- Use a strict JSON schema response with fields for:
  - `class_id`
  - `class_name`
  - `score`
  - `bbox_xyxy`
  - `color_hint`
  - `notes`
  - `same_or_unique`
  - `matched_track_id` when applicable
- Keep the model output deterministic:
  - temperature `0`
  - top-p `1`
  - strict schema validation before any DB write
- The prompt should instruct the model to:
  - detect only construction-relevant objects
  - ignore road and parking-lot vehicles
  - return one object per visible instance
  - prefer existing taxonomy classes only
  - use `same` only when correspondence is clear
- Use a small controlled vocabulary for `color_hint`, such as `yellow`, `orange`, `white`, `red`, `blue`, `green`, `black`, `gray`, `unknown`.
- Keep `notes` short and bounded so it remains useful for debugging and display, not free-form commentary.
- The prompt should be built from:
  - current image
  - current project/camera context
  - active track summary from the database
  - the allowed class list
  - the color vocabulary
  - the association rules for `same` vs `unique`

## Code Changes
- Introduce a detection strategy layer under the detection application or infrastructure boundary.
- Keep the current YOLO gateway implementation intact and adapt it behind the strategy.
- Add an OpenAI detection gateway/service that converts image bytes into the structured detection payload.
- Extend the persistence adapter so it can:
  - insert/update analysis runs
  - write detections with `track_id` and `analysis_run_id`
  - create or update tracks
  - mark track association state based on the latest image
- Keep the existing `DetectionPostProcessor` only where it still makes sense for geometry validation, clamping, and image quality checks; do not rely on it as the main semantic filter anymore.
- Add a repository or query service for assembling the previous-frame context summary for OpenAI.
- Keep the public API unchanged unless the current detection endpoints need a small response field update for run metadata.

## Testing
- Add migration tests or schema validation for:
  - `detection_classes`
  - `detection_tracks`
  - `detection_analysis_runs`
  - new `detections` columns
- Add parser tests for:
  - valid OpenAI JSON
  - missing bbox
  - unknown class ids
  - duplicate objects
  - invalid colors
  - malformed JSON
- Add strategy tests for:
  - OpenAI success path
  - OpenAI retry path
  - YOLO fallback on third failure
  - provider `yolo` mode with no OpenAI call
- Add association tests for:
  - first image creating tracks
  - second image matching existing tracks
  - new object creating a new track
  - object disappearance leaving a track inactive rather than deleting it
- Add a small dataset-based evaluation test or manual verification script against real construction images to compare OpenAI output quality against current YOLO results.

## Assumptions
- OpenAI is the primary detection engine for the MVP.
- YOLO remains present only as fallback or explicit `yolo` mode.
- No backward compatibility is required for the new taxonomy table, so `class_name` does not need to be duplicated on `detections`.
- The previous image itself is not sent to OpenAI in the MVP; normalized prior detections from Postgres are sufficient and cheaper.
- Shadow mode is out of scope because the concept was already tested manually.

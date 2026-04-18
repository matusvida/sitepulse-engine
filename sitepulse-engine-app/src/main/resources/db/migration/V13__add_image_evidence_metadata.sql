ALTER TABLE images
    ADD COLUMN IF NOT EXISTS weather_note VARCHAR(64),
    ADD COLUMN IF NOT EXISTS evidence_activity_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS evidence_change_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS evidence_quality_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS evidence_overall_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS evidence_summary JSONB;

CREATE INDEX IF NOT EXISTS idx_images_project_captured_evidence
    ON images(project_id, captured_at DESC, evidence_overall_score DESC)
    WHERE status = 'DONE';

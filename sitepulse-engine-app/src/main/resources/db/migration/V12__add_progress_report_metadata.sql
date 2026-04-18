ALTER TABLE progress_reports
    ADD COLUMN IF NOT EXISTS generation_origin VARCHAR(16) NOT NULL DEFAULT 'manual',
    ADD COLUMN IF NOT EXISTS period_key VARCHAR(64),
    ADD COLUMN IF NOT EXISTS confidence_level VARCHAR(16) NOT NULL DEFAULT 'medium',
    ADD COLUMN IF NOT EXISTS headline VARCHAR(255),
    ADD COLUMN IF NOT EXISTS evidence_image_count INTEGER;

UPDATE progress_reports
SET report_type = COALESCE(NULLIF(report_type, ''), 'custom');

UPDATE progress_reports
SET generation_origin = 'manual'
WHERE generation_origin IS NULL OR generation_origin = '';

UPDATE progress_reports
SET confidence_level = 'medium'
WHERE confidence_level IS NULL OR confidence_level = '';

UPDATE progress_reports
SET headline = summary
WHERE headline IS NULL AND summary IS NOT NULL;

UPDATE progress_reports
SET evidence_image_count = image_count
WHERE evidence_image_count IS NULL;

UPDATE progress_reports
SET period_key = CONCAT(
    report_type,
    ':',
    COALESCE(date_range_start::text, 'none'),
    ':',
    COALESCE(date_range_end::text, 'none')
)
WHERE period_key IS NULL;

ALTER TABLE progress_reports
    DROP CONSTRAINT IF EXISTS progress_reports_generation_origin_chk;

ALTER TABLE progress_reports
    ADD CONSTRAINT progress_reports_generation_origin_chk
        CHECK (generation_origin IN ('automatic', 'manual'));

ALTER TABLE progress_reports
    DROP CONSTRAINT IF EXISTS progress_reports_confidence_level_chk;

ALTER TABLE progress_reports
    ADD CONSTRAINT progress_reports_confidence_level_chk
        CHECK (confidence_level IN ('high', 'medium', 'low'));

CREATE INDEX IF NOT EXISTS idx_progress_reports_project_type_created_at
    ON progress_reports(project_id, report_type, created_at DESC);

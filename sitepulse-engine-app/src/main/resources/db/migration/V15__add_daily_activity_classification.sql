ALTER TABLE daily_metrics
    ADD COLUMN IF NOT EXISTS activity_status VARCHAR(16),
    ADD COLUMN IF NOT EXISTS activity_confidence VARCHAR(16),
    ADD COLUMN IF NOT EXISTS weather_status VARCHAR(24),
    ADD COLUMN IF NOT EXISTS weather_impacted BOOLEAN,
    ADD COLUMN IF NOT EXISTS reason_codes JSONB,
    ADD COLUMN IF NOT EXISTS summary_note VARCHAR(512);

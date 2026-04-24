ALTER TABLE progress_reports
    ADD COLUMN IF NOT EXISTS language VARCHAR(8) NOT NULL DEFAULT 'sk';

UPDATE progress_reports
SET language = 'sk'
WHERE language IS NULL OR language = '';

ALTER TABLE progress_reports
    DROP CONSTRAINT IF EXISTS progress_reports_language_chk;

ALTER TABLE progress_reports
    ADD CONSTRAINT progress_reports_language_chk
        CHECK (language IN ('sk', 'en'));

DROP INDEX IF EXISTS uq_progress_reports_automatic_period;

CREATE UNIQUE INDEX IF NOT EXISTS uq_progress_reports_automatic_period_language
    ON progress_reports(project_id, period_key, language)
    WHERE generation_origin = 'automatic' AND period_key IS NOT NULL;

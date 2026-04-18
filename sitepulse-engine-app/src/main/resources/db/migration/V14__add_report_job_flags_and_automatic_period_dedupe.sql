INSERT INTO job_feature_flags (job_name, enabled)
VALUES
    ('automaticDailyReportJob', FALSE),
    ('automaticWeeklyReportJob', FALSE)
ON CONFLICT (job_name) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS uq_progress_reports_automatic_period
    ON progress_reports(project_id, period_key)
    WHERE generation_origin = 'automatic' AND period_key IS NOT NULL;

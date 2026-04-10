UPDATE cameras c
SET dropbox_path = p.dropbox_path
FROM projects p
WHERE c.project_id = p.id
  AND c.dropbox_path IS NULL
  AND p.dropbox_path IS NOT NULL;

ALTER TABLE projects
    DROP COLUMN IF EXISTS dropbox_path;

CREATE TABLE IF NOT EXISTS job_feature_flags (
    job_name VARCHAR(128) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO job_feature_flags (job_name, enabled)
VALUES
    ('dropboxSyncJob', FALSE),
    ('detectionSweepJob', FALSE),
    ('nightlyAnalysisJob', FALSE)
ON CONFLICT (job_name) DO NOTHING;

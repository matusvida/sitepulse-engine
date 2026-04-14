ALTER TABLE detections
    ADD COLUMN IF NOT EXISTS in_roi_bool BOOLEAN;

UPDATE detections
SET in_roi_bool = CASE
        WHEN in_roi IS NULL THEN NULL
        WHEN LOWER(in_roi) = 'true' THEN TRUE
        WHEN LOWER(in_roi) = 'false' THEN FALSE
        ELSE NULL
    END
WHERE in_roi_bool IS NULL;

ALTER TABLE detections
    DROP COLUMN IF EXISTS in_roi;

ALTER TABLE detections
    RENAME COLUMN in_roi_bool TO in_roi;

ALTER TABLE detections
    DROP COLUMN IF EXISTS class_name;

ALTER TABLE detections
    DROP CONSTRAINT IF EXISTS detections_score_range_chk;

ALTER TABLE detections
    ADD CONSTRAINT detections_score_range_chk
        CHECK (score IS NULL OR (score >= 0.0 AND score <= 1.0));

CREATE UNIQUE INDEX IF NOT EXISTS uq_images_bucket_key
    ON images(bucket, key);

ALTER TABLE detections
    DROP CONSTRAINT IF EXISTS detections_image_id_fkey;

ALTER TABLE detections
    ADD CONSTRAINT detections_image_id_fkey
        FOREIGN KEY (image_id) REFERENCES images(id)
        ON DELETE CASCADE;

ALTER TABLE detections
    DROP CONSTRAINT IF EXISTS detections_track_id_fkey;

ALTER TABLE detections
    ADD CONSTRAINT detections_track_id_fkey
        FOREIGN KEY (track_id) REFERENCES detection_tracks(id)
        ON DELETE SET NULL;

ALTER TABLE detections
    DROP CONSTRAINT IF EXISTS detections_analysis_run_id_fkey;

ALTER TABLE detections
    ADD CONSTRAINT detections_analysis_run_id_fkey
        FOREIGN KEY (analysis_run_id) REFERENCES detection_analysis_runs(id)
        ON DELETE CASCADE;

ALTER TABLE detection_analysis_runs
    DROP CONSTRAINT IF EXISTS detection_analysis_runs_image_id_fkey;

ALTER TABLE detection_analysis_runs
    ADD CONSTRAINT detection_analysis_runs_image_id_fkey
        FOREIGN KEY (image_id) REFERENCES images(id)
        ON DELETE CASCADE;

ALTER TABLE detection_analysis_runs
    DROP CONSTRAINT IF EXISTS detection_analysis_runs_previous_image_id_fkey;

ALTER TABLE detection_analysis_runs
    ADD CONSTRAINT detection_analysis_runs_previous_image_id_fkey
        FOREIGN KEY (previous_image_id) REFERENCES images(id)
        ON DELETE SET NULL;

ALTER TABLE detection_tracks
    DROP CONSTRAINT IF EXISTS detection_tracks_first_seen_image_id_fkey;

ALTER TABLE detection_tracks
    ADD CONSTRAINT detection_tracks_first_seen_image_id_fkey
        FOREIGN KEY (first_seen_image_id) REFERENCES images(id)
        ON DELETE SET NULL;

ALTER TABLE detection_tracks
    DROP CONSTRAINT IF EXISTS detection_tracks_last_seen_image_id_fkey;

ALTER TABLE detection_tracks
    ADD CONSTRAINT detection_tracks_last_seen_image_id_fkey
        FOREIGN KEY (last_seen_image_id) REFERENCES images(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_detections_image_id
    ON detections(image_id);

CREATE INDEX IF NOT EXISTS idx_detection_analysis_runs_previous_image_id
    ON detection_analysis_runs(previous_image_id);

CREATE TABLE IF NOT EXISTS detection_classes (
    id INTEGER PRIMARY KEY,
    class_name VARCHAR(128) NOT NULL UNIQUE
);

INSERT INTO detection_classes (id, class_name) VALUES
    (0, 'unknown'),
    (1, 'person'),
    (2, 'worker'),
    (3, 'operator'),
    (4, 'supervisor'),
    (5, 'car'),
    (6, 'van'),
    (7, 'pickup_truck'),
    (8, 'truck'),
    (9, 'dump_truck'),
    (10, 'concrete_mixer_truck'),
    (11, 'tanker_truck'),
    (12, 'bus'),
    (13, 'motorcycle'),
    (14, 'bicycle'),
    (15, 'trailer'),
    (16, 'excavator'),
    (17, 'mini_excavator'),
    (18, 'backhoe_loader'),
    (19, 'wheel_loader'),
    (20, 'skid_steer_loader'),
    (21, 'bulldozer'),
    (22, 'grader'),
    (23, 'roller'),
    (24, 'forklift'),
    (25, 'telehandler'),
    (26, 'paver'),
    (27, 'crane_mobile'),
    (28, 'crane_tower'),
    (29, 'crane_truck'),
    (30, 'hoist'),
    (31, 'cherry_picker'),
    (32, 'scaffolding'),
    (33, 'generator'),
    (34, 'helicopter'),
    (35, 'other_vehicle'),
    (36, 'other_equipment')
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS detection_tracks (
    id SERIAL PRIMARY KEY,
    project_id INTEGER REFERENCES projects(id),
    camera_id INTEGER REFERENCES cameras(id),
    class_id INTEGER NOT NULL REFERENCES detection_classes(id),
    color_hint VARCHAR(32),
    current_bbox_xyxy TEXT,
    first_seen_image_id INTEGER REFERENCES images(id),
    last_seen_image_id INTEGER REFERENCES images(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS detection_analysis_runs (
    id SERIAL PRIMARY KEY,
    image_id INTEGER NOT NULL REFERENCES images(id),
    provider VARCHAR(32) NOT NULL,
    model_version VARCHAR(128),
    prompt_version VARCHAR(64),
    retry_count INTEGER DEFAULT 0,
    previous_image_id INTEGER REFERENCES images(id),
    status VARCHAR(32) NOT NULL,
    latency_ms DOUBLE PRECISION,
    error TEXT,
    raw_response JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE detections
    ADD COLUMN IF NOT EXISTS color_hint VARCHAR(32),
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS track_id INTEGER REFERENCES detection_tracks(id),
    ADD COLUMN IF NOT EXISTS analysis_run_id INTEGER REFERENCES detection_analysis_runs(id);

UPDATE detections
SET class_id = NULL
WHERE class_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM detection_classes c
      WHERE c.id = detections.class_id
  );

ALTER TABLE detections
    ADD CONSTRAINT fk_detections_class_id
        FOREIGN KEY (class_id) REFERENCES detection_classes(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_detection_tracks_project_camera ON detection_tracks(project_id, camera_id);
CREATE INDEX IF NOT EXISTS idx_detection_tracks_last_seen ON detection_tracks(last_seen_image_id);
CREATE INDEX IF NOT EXISTS idx_detection_analysis_runs_image ON detection_analysis_runs(image_id);
CREATE INDEX IF NOT EXISTS idx_detections_track_id ON detections(track_id);
CREATE INDEX IF NOT EXISTS idx_detections_analysis_run_id ON detections(analysis_run_id);

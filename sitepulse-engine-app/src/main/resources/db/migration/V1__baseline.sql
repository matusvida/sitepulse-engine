CREATE TABLE IF NOT EXISTS projects (
    id SERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    location VARCHAR(512),
    dropbox_path VARCHAR(1024),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cameras (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    name VARCHAR(256),
    roi_polygon JSONB,
    drop_outside BOOLEAN DEFAULT TRUE,
    key_prefix VARCHAR(512),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS images (
    id SERIAL PRIMARY KEY,
    bucket VARCHAR(256) NOT NULL,
    key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    project_id INTEGER REFERENCES projects(id),
    camera_id INTEGER REFERENCES cameras(id),
    captured_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS detections (
    id SERIAL PRIMARY KEY,
    image_id INTEGER NOT NULL REFERENCES images(id),
    project_id INTEGER REFERENCES projects(id),
    model_version VARCHAR(128),
    class_id INTEGER,
    class_name VARCHAR(128),
    score DOUBLE PRECISION,
    bbox_xyxy TEXT,
    in_roi VARCHAR(8),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS daily_metrics (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    date DATE NOT NULL,
    people_count INTEGER,
    vehicle_count INTEGER,
    active_hours DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_daily_metrics_project_date UNIQUE (project_id, date)
);

CREATE TABLE IF NOT EXISTS weekly_metrics (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    week_start DATE NOT NULL,
    progress_delta DOUBLE PRECISION,
    activity_index DOUBLE PRECISION,
    active_hours DOUBLE PRECISION,
    risk_level VARCHAR(16),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_weekly_metrics_project_week UNIQUE (project_id, week_start)
);

CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    type VARCHAR(32),
    severity VARCHAR(16),
    status VARCHAR(16) DEFAULT 'open',
    summary TEXT,
    details TEXT,
    recommended_actions JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS sync_jobs (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    status VARCHAR(32),
    images_found INTEGER,
    images_synced INTEGER,
    error TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS construction_plans (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    filename VARCHAR(512),
    raw_text TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'processing',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS plan_milestones (
    id SERIAL PRIMARY KEY,
    plan_id INTEGER NOT NULL REFERENCES construction_plans(id) ON DELETE CASCADE,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    week_number INTEGER NOT NULL,
    title VARCHAR(512) NOT NULL,
    description TEXT,
    expected_state TEXT,
    actual_state TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'not_started',
    checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS progress_reports (
    id SERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id),
    report_type VARCHAR(32) NOT NULL DEFAULT 'custom',
    content_md TEXT,
    summary TEXT,
    date_range_start DATE,
    date_range_end DATE,
    image_count INTEGER,
    model_used VARCHAR(128),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS camera_snapshot_profiles (
    id BIGSERIAL PRIMARY KEY,
    camera_id INTEGER NOT NULL UNIQUE REFERENCES cameras(id) ON DELETE CASCADE,
    target_width INTEGER NOT NULL DEFAULT 1920,
    target_quality INTEGER NOT NULL DEFAULT 75,
    target_format VARCHAR(32) NOT NULL DEFAULT 'webp',
    freeze_time TIME NOT NULL DEFAULT TIME '17:00:00',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS camera_daily_snapshots (
    id BIGSERIAL PRIMARY KEY,
    camera_id INTEGER NOT NULL REFERENCES cameras(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    source_image_id INTEGER NOT NULL REFERENCES images(id),
    bucket VARCHAR(256) NOT NULL,
    key VARCHAR(1024) NOT NULL,
    media_type VARCHAR(128) NOT NULL,
    is_frozen BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_camera_daily_snapshots_camera_date UNIQUE (camera_id, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_camera_daily_snapshots_camera_id
    ON camera_daily_snapshots(camera_id);

CREATE INDEX IF NOT EXISTS idx_camera_daily_snapshots_snapshot_date
    ON camera_daily_snapshots(snapshot_date);

CREATE INDEX IF NOT EXISTS idx_camera_daily_snapshots_camera_date_frozen
    ON camera_daily_snapshots(camera_id, snapshot_date, is_frozen);

INSERT INTO camera_snapshot_profiles (
    camera_id,
    target_width,
    target_quality,
    target_format,
    freeze_time,
    created_at,
    updated_at
)
SELECT
    c.id,
    1920,
    75,
    'webp',
    TIME '17:00:00',
    NOW(),
    NOW()
FROM cameras c
WHERE NOT EXISTS (
    SELECT 1
    FROM camera_snapshot_profiles p
    WHERE p.camera_id = c.id
);

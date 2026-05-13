CREATE TABLE IF NOT EXISTS reports_images (
    id SERIAL PRIMARY KEY,
    report_id INTEGER NOT NULL REFERENCES progress_reports(id) ON DELETE CASCADE,
    image_id INTEGER NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    image_path VARCHAR(1024) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_images_report_id ON reports_images(report_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_reports_images_report_image ON reports_images(report_id, image_id);

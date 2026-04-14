-- PostgreSQL does not guarantee meaningful physical column order for ALTER TABLE ADD COLUMN.
-- This migration adds the project timezone safely and keeps the schema compatible with existing rows.
ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(64);

UPDATE projects
SET timezone = 'Europe/Bratislava'
WHERE id = 1
  AND (timezone IS NULL OR timezone <> 'Europe/Bratislava');

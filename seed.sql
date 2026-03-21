-- Seed data for the first project + camera with ROI polygon.
-- Run: psql $POSTGRES_DSN -f seed.sql
-- Or:  docker compose exec postgres psql -U sitepulse -d sitepulse -f /seed.sql

INSERT INTO projects (id, name, location, dropbox_path, created_at)
VALUES (1, 'Tower TL', 'Prague, Czech Republic', NULL, NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, location = EXCLUDED.location;

INSERT INTO cameras (id, project_id, name, roi_polygon, drop_outside, key_prefix, created_at)
VALUES (
  1,
  1,
  'Camera 1',
  '[[9, 1568], [2348, 0], [4614, 13], [4614, 2686]]'::jsonb,
  true,
  'tower-tl/',
  NOW()
)
ON CONFLICT (id) DO UPDATE
  SET roi_polygon = EXCLUDED.roi_polygon,
      name       = EXCLUDED.name,
      key_prefix = EXCLUDED.key_prefix;

-- Reset the sequences so the next auto-generated IDs don't collide
SELECT setval('projects_id_seq', (SELECT COALESCE(MAX(id), 0) FROM projects));
SELECT setval('cameras_id_seq',  (SELECT COALESCE(MAX(id), 0) FROM cameras));

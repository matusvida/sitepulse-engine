ALTER TABLE projects
    ADD COLUMN storage_key_prefix VARCHAR(512);

ALTER TABLE cameras
    ADD COLUMN dropbox_path VARCHAR(1024);

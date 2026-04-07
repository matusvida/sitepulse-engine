ALTER TABLE images
    ADD COLUMN IF NOT EXISTS image_width INTEGER,
    ADD COLUMN IF NOT EXISTS image_height INTEGER;

CREATE INDEX IF NOT EXISTS idx_images_dimensions
    ON images(image_width, image_height);

ALTER TABLE detection_classes
    ADD COLUMN IF NOT EXISTS class_group VARCHAR(64);

UPDATE detection_classes
SET class_group = CASE class_name
    WHEN 'unknown' THEN 'unknown'
    WHEN 'person' THEN 'people'
    WHEN 'worker' THEN 'people'
    WHEN 'operator' THEN 'people'
    WHEN 'supervisor' THEN 'people'
    WHEN 'car' THEN 'light_vehicle'
    WHEN 'van' THEN 'light_vehicle'
    WHEN 'pickup_truck' THEN 'light_vehicle'
    WHEN 'truck' THEN 'truck'
    WHEN 'dump_truck' THEN 'truck'
    WHEN 'concrete_mixer_truck' THEN 'truck'
    WHEN 'tanker_truck' THEN 'truck'
    WHEN 'crane_truck' THEN 'truck'
    WHEN 'bus' THEN 'transport'
    WHEN 'motorcycle' THEN 'transport'
    WHEN 'bicycle' THEN 'transport'
    WHEN 'trailer' THEN 'transport'
    WHEN 'excavator' THEN 'earthmoving'
    WHEN 'mini_excavator' THEN 'earthmoving'
    WHEN 'backhoe_loader' THEN 'earthmoving'
    WHEN 'wheel_loader' THEN 'earthmoving'
    WHEN 'skid_steer_loader' THEN 'earthmoving'
    WHEN 'bulldozer' THEN 'earthmoving'
    WHEN 'grader' THEN 'earthmoving'
    WHEN 'roller' THEN 'earthmoving'
    WHEN 'forklift' THEN 'lifting'
    WHEN 'telehandler' THEN 'lifting'
    WHEN 'crane_mobile' THEN 'lifting'
    WHEN 'crane_tower' THEN 'lifting'
    WHEN 'hoist' THEN 'lifting'
    WHEN 'cherry_picker' THEN 'lifting'
    WHEN 'paver' THEN 'paving'
    WHEN 'scaffolding' THEN 'structure'
    WHEN 'generator' THEN 'power'
    WHEN 'helicopter' THEN 'aerial'
    WHEN 'other_vehicle' THEN 'other_vehicle'
    WHEN 'other_equipment' THEN 'other_equipment'
    ELSE 'unknown'
END
WHERE class_group IS NULL;

ALTER TABLE detection_classes
    ALTER COLUMN class_group SET NOT NULL;

ALTER TABLE detection_classes
    DROP CONSTRAINT IF EXISTS detection_classes_class_group_chk;

ALTER TABLE detection_classes
    ADD CONSTRAINT detection_classes_class_group_chk
        CHECK (class_group IN (
            'people',
            'light_vehicle',
            'truck',
            'transport',
            'earthmoving',
            'lifting',
            'paving',
            'structure',
            'power',
            'aerial',
            'other_vehicle',
            'other_equipment',
            'unknown'
        ));

ALTER TABLE weekly_metrics
    ALTER COLUMN progress_delta TYPE NUMERIC(8,1)
        USING ROUND(progress_delta::numeric, 1),
    ALTER COLUMN activity_index TYPE NUMERIC(8,1)
        USING ROUND(activity_index::numeric, 1);

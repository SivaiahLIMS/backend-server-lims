ALTER TABLE sample_batch
    ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE sample_batch
    ADD CONSTRAINT fk_sample_batch_created_by
        FOREIGN KEY (created_by)
            REFERENCES app_user(id)
            ON DELETE SET NULL;


ALTER TABLE sample_batch
    ADD COLUMN IF NOT EXISTS created_by BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_sample_batch_created_by'
          AND table_name = 'sample_batch'
    ) THEN
ALTER TABLE sample_batch
    ADD CONSTRAINT fk_sample_batch_created_by
        FOREIGN KEY (created_by)
            REFERENCES app_user(id)
            ON DELETE SET NULL;
END IF;
END $$;
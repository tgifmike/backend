-- Run this migration against databases that already have line_check_items.
-- Hibernate's local ddl-auto=update may create these columns locally, but this
-- script makes the production schema change explicit and repeatable.
ALTER TABLE line_check_items
    ADD COLUMN IF NOT EXISTS is_corrected BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE line_check_items
    ADD COLUMN IF NOT EXISTS corrective_notes TEXT;

ALTER TABLE line_check_items
    ADD COLUMN IF NOT EXISTS corrected_at TIMESTAMPTZ;

ALTER TABLE line_check_items
    ADD COLUMN IF NOT EXISTS corrected_by UUID;

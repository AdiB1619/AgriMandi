-- Schema/entity alignment for BaseEntity auditing columns
--
-- Every entity extends BaseEntity, which declares a non-null created_at and a
-- non-null updated_at maintained by JPA auditing. The earlier create-table
-- migrations (V6, V7, V9) omitted those columns on weighments, bids,
-- asset_transactions and audit_logs, so the application could not start against
-- a real PostgreSQL database: with spring.jpa.hibernate.ddl-auto=validate the
-- schema check fails with "missing column [updated_at] in table
-- [asset_transactions]" before any request is served.
--
-- Existing rows are backfilled from their own business timestamp (or from the
-- table creation time) so the NOT NULL constraint can be applied safely, and the
-- defaults keep direct SQL inserts working.

ALTER TABLE weighments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE weighments SET updated_at = COALESCE(weighment_time, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL;
ALTER TABLE weighments ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE weighments ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE bids ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE bids SET updated_at = COALESCE(bid_time, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL;
ALTER TABLE bids ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE bids ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE asset_transactions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE asset_transactions SET updated_at = COALESCE(movement_date, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL;
ALTER TABLE asset_transactions ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE asset_transactions ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- audit_logs was created without any BaseEntity auditing columns at all.
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE audit_logs SET created_at = COALESCE(timestamp, CURRENT_TIMESTAMP) WHERE created_at IS NULL;
UPDATE audit_logs SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE audit_logs ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE audit_logs ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Schema/entity alignment for the payments table
--
-- Same defect as V13: BaseEntity declares a non-null updated_at maintained by
-- JPA auditing, but V9 created payments without it. With
-- spring.jpa.hibernate.ddl-auto=validate the application fails to start with
-- "missing column [updated_at] in table [payments]".
--
-- V13 could not cover this table, so the fix ships separately rather than
-- editing an already-applied, checksummed migration.

ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE payments SET updated_at = COALESCE(payment_date, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL;
ALTER TABLE payments ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE payments ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

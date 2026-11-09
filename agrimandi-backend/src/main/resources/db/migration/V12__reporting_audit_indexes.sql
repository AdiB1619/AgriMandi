-- Phase: Reports, Admin Oversight, Audit Trail
-- Adds the minimum audit metadata required to expose a market-scoped audit trail,
-- a durable tie flag on auctions (the auction status is overwritten once the winner
-- is confirmed, which would otherwise make tie counts unmeasurable), and the indexes
-- required by the server-side report aggregations.

ALTER TABLE audit_logs ADD COLUMN actor_role VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN market_id UUID REFERENCES markets(id);

CREATE INDEX idx_audit_logs_market_timestamp ON audit_logs(market_id, timestamp DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);

ALTER TABLE auctions ADD COLUMN tie_detected BOOLEAN DEFAULT FALSE;

CREATE INDEX idx_transactions_market_date ON transactions(lot_id, transaction_date DESC);
CREATE INDEX idx_payments_payment_date ON payments(payment_date DESC);
CREATE INDEX idx_asset_transactions_movement_date ON asset_transactions(movement_date DESC);
CREATE INDEX idx_auctions_market_started ON auctions(started_at DESC);
CREATE INDEX idx_lots_market_created ON lots(market_id, created_at DESC);

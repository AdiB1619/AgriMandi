-- Restore the documented demo credentials
--
-- V11 states that its BCrypt hash is the hash of 'password', but it is not:
-- every seeded account (rajesh.patel, admin.demo, agent.demo, farmer.demo,
-- buyer.demo) is stored with a hash that does not verify, so POST
-- /api/auth/login returns 401 for all of them and the demo environment cannot
-- be signed into.
--
-- V11 is already applied and therefore checksummed, so the correction ships as
-- a new migration instead of an edit. The replacement hash below was produced
-- with the application's own BCryptPasswordEncoder and verifies against
-- 'password'.
--
-- These are demo/seed accounts only. Real deployments must not rely on this
-- shared password; rotate it out of band.

UPDATE users
SET password_hash = '$2a$10$UQzmLkRtr4JAI229OgdEUuV6VKbMmNEzFO4ohE6GCtO130tkjJbjS'
WHERE username IN ('rajesh.patel', 'admin.demo', 'agent.demo', 'farmer.demo', 'buyer.demo');

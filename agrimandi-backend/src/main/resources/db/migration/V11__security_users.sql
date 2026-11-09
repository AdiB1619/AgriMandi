ALTER TABLE users
ADD COLUMN farmer_id UUID REFERENCES farmers(id),
ADD COLUMN buyer_id UUID REFERENCES buyers(id);

-- BCrypt hash for 'password'
-- $2a$10$8.UnVuG9HLDAjL3J1yYW2.macWEeRPE/Wn1Y6x34QoA9g51l7Xb1S

UPDATE users SET password_hash = '$2a$10$8.UnVuG9HLDAjL3J1yYW2.macWEeRPE/Wn1Y6x34QoA9g51l7Xb1S' WHERE username = 'rajesh.patel';

-- Create admin.demo (Market id 11111111-1111-1111-1111-111111111111 is the demo market)
INSERT INTO users (id, market_id, username, password_hash, role, status) VALUES 
('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 'admin.demo', '$2a$10$8.UnVuG9HLDAjL3J1yYW2.macWEeRPE/Wn1Y6x34QoA9g51l7Xb1S', 'ADMIN', 'ACTIVE');

-- Create agent.demo
INSERT INTO users (id, market_id, username, password_hash, role, status) VALUES 
('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', 'agent.demo', '$2a$10$8.UnVuG9HLDAjL3J1yYW2.macWEeRPE/Wn1Y6x34QoA9g51l7Xb1S', 'AGENT', 'ACTIVE');

-- Create farmer.demo linked to FRM-001
INSERT INTO users (id, market_id, username, password_hash, role, status, farmer_id) VALUES 
('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111111', 'farmer.demo', '$2a$10$8.UnVuG9HLDAjL3J1yYW2.macWEeRPE/Wn1Y6x34QoA9g51l7Xb1S', 'FARMER', 'ACTIVE', '55555555-5555-5555-5555-555555555551');

-- Create buyer.demo linked to BYR-001
INSERT INTO users (id, market_id, username, password_hash, role, status, buyer_id) VALUES 
('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111111', 'buyer.demo', '$2a$10$8.UnVuG9HLDAjL3J1yYW2.macWEeRPE/Wn1Y6x34QoA9g51l7Xb1S', 'BUYER', 'ACTIVE', '66666666-6666-6666-6666-666666666661');

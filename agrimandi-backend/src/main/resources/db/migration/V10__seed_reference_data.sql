-- Insert Demo Market
INSERT INTO markets (id, code, name, location) VALUES 
('11111111-1111-1111-1111-111111111111', 'APMC-PUN', 'Pune APMC Market Yard', 'Pune, Maharashtra');

-- Insert Demo Admin/Agent User
INSERT INTO users (id, market_id, username, role) VALUES 
('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'rajesh.patel', 'AGENT');

-- Insert Commodities
INSERT INTO commodities (id, name, code, default_price_unit) VALUES 
('33333333-3333-3333-3333-333333333331', 'Tomato', 'TOM-01', 'KG'),
('33333333-3333-3333-3333-333333333332', 'Onion', 'ONN-01', 'KG'),
('33333333-3333-3333-3333-333333333333', 'Coriander', 'COR-01', 'BUNDLE'),
('33333333-3333-3333-3333-333333333334', 'Banana', 'BAN-01', 'DOZEN');

-- Insert Asset Types
INSERT INTO asset_types (id, market_id, name, code, returnable) VALUES 
('44444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111111', 'Plastic Crate (Red)', 'CRT-RED', TRUE),
('44444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111111', 'Jute Sack', 'SCK-JUT', TRUE);

-- Insert Farmers
INSERT INTO farmers (id, market_id, farmer_code, name, village, phone) VALUES 
('55555555-5555-5555-5555-555555555551', '11111111-1111-1111-1111-111111111111', 'FRM-001', 'Rameshwar Khot', 'Phaltan, Satara', '9876543210'),
('55555555-5555-5555-5555-555555555552', '11111111-1111-1111-1111-111111111111', 'FRM-002', 'Vithal Gaikwad', 'Khed Shivapur, Pune', '9876543211'),
('55555555-5555-5555-5555-555555555553', '11111111-1111-1111-1111-111111111111', 'FRM-003', 'Suresh Jadhav', 'Baramati, Pune', '9876543212');

-- Insert Buyers
INSERT INTO buyers (id, market_id, buyer_code, name, business_name, license_reference) VALUES 
('66666666-6666-6666-6666-666666666661', '11111111-1111-1111-1111-111111111111', 'BYR-001', 'ABC Agro Traders', 'ABC Agro Traders', 'LIC-088'),
('66666666-6666-6666-6666-666666666662', '11111111-1111-1111-1111-111111111111', 'BYR-002', 'Kisan B2B Supply', 'Kisan B2B Supply', 'LIC-014'),
('66666666-6666-6666-6666-666666666663', '11111111-1111-1111-1111-111111111111', 'BYR-003', 'Mauli Veg Wholesalers', 'Mauli Veg Wholesalers', 'LIC-112'),
('66666666-6666-6666-6666-666666666664', '11111111-1111-1111-1111-111111111111', 'BYR-004', 'Sahyadri Fresh Produce', 'Sahyadri Fresh Produce', 'LIC-042');

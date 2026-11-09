CREATE TABLE weighments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id UUID NOT NULL REFERENCES lots(id),
    gross_weight DECIMAL(19, 4),
    tare_weight DECIMAL(19, 4),
    net_weight DECIMAL(19, 4) NOT NULL,
    weight_unit VARCHAR(50) NOT NULL,
    weighment_time TIMESTAMP WITH TIME ZONE,
    weighment_reference VARCHAR(100),
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_weighments_lot_id ON weighments(lot_id);

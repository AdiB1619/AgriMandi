CREATE TABLE asset_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    market_id UUID NOT NULL REFERENCES markets(id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    returnable BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_number VARCHAR(100) NOT NULL UNIQUE,
    market_id UUID NOT NULL REFERENCES markets(id),
    farmer_id UUID NOT NULL REFERENCES farmers(id),
    commodity_id UUID NOT NULL REFERENCES commodities(id),
    quantity DECIMAL(19, 4) NOT NULL,
    quantity_unit VARCHAR(50) NOT NULL,
    packaging_type VARCHAR(50) NOT NULL,
    asset_type_id UUID REFERENCES asset_types(id),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lots_market_id ON lots(market_id);
CREATE INDEX idx_lots_farmer_id ON lots(farmer_id);
CREATE INDEX idx_lots_commodity_id ON lots(commodity_id);
CREATE INDEX idx_lots_status ON lots(status);

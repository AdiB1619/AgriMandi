CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_number VARCHAR(100) NOT NULL UNIQUE,
    lot_id UUID NOT NULL REFERENCES lots(id),
    auction_id UUID NOT NULL REFERENCES auctions(id),
    farmer_id UUID NOT NULL REFERENCES farmers(id),
    buyer_id UUID NOT NULL REFERENCES buyers(id),
    winning_bid_id UUID NOT NULL REFERENCES bids(id),
    quantity DECIMAL(19, 4) NOT NULL,
    quantity_unit VARCHAR(50) NOT NULL,
    winning_rate DECIMAL(19, 4) NOT NULL,
    price_unit VARCHAR(50) NOT NULL,
    gross_amount DECIMAL(19, 4) NOT NULL,
    adjustments DECIMAL(19, 4) DEFAULT 0,
    net_amount DECIMAL(19, 4) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_lot_id ON transactions(lot_id);
CREATE INDEX idx_transactions_farmer_id ON transactions(farmer_id);
CREATE INDEX idx_transactions_buyer_id ON transactions(buyer_id);
CREATE INDEX idx_transactions_transaction_number ON transactions(transaction_number);

CREATE TABLE auctions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id UUID NOT NULL REFERENCES lots(id),
    status VARCHAR(50) NOT NULL DEFAULT 'READY',
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    winner_bid_id UUID,
    winner_buyer_id UUID REFERENCES buyers(id),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auctions_lot_id ON auctions(lot_id);

CREATE TABLE bids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auction_id UUID NOT NULL REFERENCES auctions(id),
    buyer_id UUID NOT NULL REFERENCES buyers(id),
    bid_amount DECIMAL(19, 4) NOT NULL,
    price_unit VARCHAR(50) NOT NULL,
    bid_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sequence_number INT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bids_auction_id ON bids(auction_id);
CREATE INDEX idx_bids_buyer_id ON bids(buyer_id);

-- Cannot add foreign key from auctions(winner_bid_id) to bids(id) during creation of auctions table because bids table didn't exist. Add it now.
ALTER TABLE auctions ADD CONSTRAINT fk_auctions_winner_bid_id FOREIGN KEY (winner_bid_id) REFERENCES bids(id);

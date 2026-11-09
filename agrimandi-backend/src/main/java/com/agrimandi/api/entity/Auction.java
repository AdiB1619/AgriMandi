package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class Auction extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;

    /** Set to true by closeAuction() when more than one active bid tied at the highest amount. */
    private Boolean tieDetected;

    @ManyToOne
    @JoinColumn(name = "winner_bid_id")
    private Bid winnerBid;

    @ManyToOne
    @JoinColumn(name = "winner_buyer_id")
    private Buyer winnerBuyer;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}

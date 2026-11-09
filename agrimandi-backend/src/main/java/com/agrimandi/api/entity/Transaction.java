package com.agrimandi.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction extends BaseEntity {

    private String transactionNumber;

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @ManyToOne
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @ManyToOne
    @JoinColumn(name = "winning_bid_id", nullable = false)
    private Bid winningBid;

    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal winningRate;
    private String priceUnit;
    
    private BigDecimal grossAmount;
    private BigDecimal adjustments;
    private BigDecimal netAmount;

    private String status;
    private OffsetDateTime transactionDate;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}

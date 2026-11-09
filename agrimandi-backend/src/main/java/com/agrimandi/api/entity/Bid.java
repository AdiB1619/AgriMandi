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
@Table(name = "bids")
@Getter
@Setter
public class Bid extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    private BigDecimal bidAmount;
    private String priceUnit;
    private OffsetDateTime bidTime;
    private Integer sequenceNumber;
    private String status;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}

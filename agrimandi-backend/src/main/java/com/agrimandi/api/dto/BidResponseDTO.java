package com.agrimandi.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class BidResponseDTO {
    private UUID id;
    private UUID auctionId;
    private UUID buyerId;
    private String buyerName;
    private BigDecimal bidAmount;
    private String priceUnit;
    private OffsetDateTime bidTime;
    private String status;
}

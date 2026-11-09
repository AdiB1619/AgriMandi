package com.agrimandi.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class TransactionResponseDTO {
    private UUID id;
    private String transactionNumber;
    
    private LotDTO lot;
    private ActorDTO farmer;
    private ActorDTO buyer;
    private BidDTO winningBid;
    
    private AmountDTO amount;
    private String paymentStatus;
    
    private AssetSummaryDTO assets;
    
    private OffsetDateTime transactionDate;
    
    @Data
    public static class LotDTO {
        private UUID id;
        private String lotNumber;
        private String commodity;
        private BigDecimal quantity;
        private String unit;
    }
    
    @Data
    public static class ActorDTO {
        private UUID id;
        private String name;
    }
    
    @Data
    public static class BidDTO {
        private UUID id;
        private BigDecimal rate;
        private String priceUnit;
    }
    
    @Data
    public static class AmountDTO {
        private BigDecimal gross;
        private BigDecimal net;
        private BigDecimal paid;
        private BigDecimal pending;
    }
    
    @Data
    public static class AssetSummaryDTO {
        private UUID assetTypeId;
        private String assetTypeName;
        private Integer issued;
        private Integer returned;
        private Integer outstanding;
    }
}

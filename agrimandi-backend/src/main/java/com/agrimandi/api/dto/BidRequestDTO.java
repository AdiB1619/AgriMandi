package com.agrimandi.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BidRequestDTO {
    @NotNull(message = "Buyer ID is required")
    private UUID buyerId;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Bid amount must be greater than zero")
    private BigDecimal bidAmount;

    @NotBlank(message = "Price unit is required")
    private String priceUnit;
}

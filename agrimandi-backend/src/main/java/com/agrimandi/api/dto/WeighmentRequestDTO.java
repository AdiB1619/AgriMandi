package com.agrimandi.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WeighmentRequestDTO {
    
    @NotNull(message = "Gross weight is required")
    @Positive(message = "Gross weight must be greater than zero")
    private BigDecimal grossWeight;
    
    @NotNull(message = "Tare weight is required")
    @PositiveOrZero(message = "Tare weight must be zero or positive")
    private BigDecimal tareWeight;
    
    private BigDecimal netWeight;
    
    @NotNull(message = "Weight unit is required")
    private String weightUnit;
}

package com.agrimandi.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String paymentReference;
    private String notes;
}

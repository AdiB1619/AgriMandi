package com.agrimandi.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WinnerConfirmationDTO {
    @NotNull(message = "Winning bid ID is required")
    private UUID winningBidId;
}

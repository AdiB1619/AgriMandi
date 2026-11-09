package com.agrimandi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Inward lot register entry.
 *
 * <p>Quantity and quantity unit are deliberately absent: a lot is created before it is weighed,
 * and {@code LotService} sets the quantity from the commodity default until the weighbridge slip
 * supplies the real net weight. Accepting a quantity here would be a value the service discards.
 */
@Data
public class LotRequestDTO {
    @NotNull(message = "Market ID is required")
    private UUID marketId;

    @NotNull(message = "Farmer ID is required")
    private UUID farmerId;

    @NotNull(message = "Commodity ID is required")
    private UUID commodityId;

    @NotBlank(message = "Packaging type is required")
    private String packagingType;

    private UUID assetTypeId;
}

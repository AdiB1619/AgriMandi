package com.agrimandi.api.dto.auth;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserResponseDTO {
    private UUID id;
    private String username;
    private String role;
    private UUID marketId;
    private UUID farmerId;
    private UUID buyerId;
}

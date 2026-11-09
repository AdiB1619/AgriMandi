package com.agrimandi.api.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponseDTO {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private int expiresIn;
    private UserResponseDTO user;
}

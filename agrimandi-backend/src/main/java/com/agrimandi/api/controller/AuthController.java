package com.agrimandi.api.controller;

import com.agrimandi.api.dto.auth.JwtResponseDTO;
import com.agrimandi.api.dto.auth.LoginRequestDTO;
import com.agrimandi.api.dto.auth.UserResponseDTO;
import com.agrimandi.api.security.CustomUserDetails;
import com.agrimandi.api.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().orElse("ROLE_USER").replace("ROLE_", "");

        return ResponseEntity.ok(JwtResponseDTO.builder()
                .accessToken(jwt)
                .expiresIn(jwtExpirationMs / 1000)
                .user(UserResponseDTO.builder()
                        .id(userDetails.getId())
                        .username(userDetails.getUsername())
                        .role(role)
                        .marketId(userDetails.getMarketId())
                        .farmerId(userDetails.getFarmerId())
                        .buyerId(userDetails.getBuyerId())
                        .build())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().orElse("ROLE_USER").replace("ROLE_", "");

        return ResponseEntity.ok(UserResponseDTO.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .role(role)
                .marketId(userDetails.getMarketId())
                .farmerId(userDetails.getFarmerId())
                .buyerId(userDetails.getBuyerId())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser() {
        // Stateless, so logout is mainly client-side token deletion
        return ResponseEntity.ok().build();
    }
}

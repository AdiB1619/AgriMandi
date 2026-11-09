package com.agrimandi.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public UUID getCurrentUserId() {
        CustomUserDetails user = getCurrentUserDetails();
        return user != null ? user.getId() : null;
    }

    public UUID getCurrentMarketId() {
        CustomUserDetails user = getCurrentUserDetails();
        return user != null ? user.getMarketId() : null;
    }

    public String getCurrentUserRole() {
        CustomUserDetails user = getCurrentUserDetails();
        if (user == null) return null;
        return user.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }
}

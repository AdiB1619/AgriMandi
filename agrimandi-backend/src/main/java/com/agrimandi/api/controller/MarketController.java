package com.agrimandi.api.controller;

import com.agrimandi.api.entity.Market;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.repository.MarketRepository;
import com.agrimandi.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only access to the market of the authenticated user.
 *
 * <p>The user may only read their own market, so the identifier is resolved from the server-side
 * principal rather than trusted from the request.
 */
@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketRepository marketRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Market> getMyMarket() {
        UUID marketId = securityUtils.getCurrentMarketId();
        if (marketId == null) {
            throw new BusinessValidationException("User is not associated with a market");
        }
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new BusinessValidationException("Market not found"));
        return ResponseEntity.ok(market);
    }
}

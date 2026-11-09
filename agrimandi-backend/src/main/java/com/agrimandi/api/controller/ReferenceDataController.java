package com.agrimandi.api.controller;

import com.agrimandi.api.dto.ReferenceDTOs.*;
import com.agrimandi.api.security.SecurityUtils;
import com.agrimandi.api.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;
    private final SecurityUtils securityUtils;

    @GetMapping("/farmers")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<List<FarmerDTO>> getFarmers() {
        UUID marketId = securityUtils.getCurrentMarketId();
        return ResponseEntity.ok(referenceDataService.getFarmersByMarket(marketId));
    }

    @GetMapping("/farmers/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<FarmerDTO> getFarmer(@PathVariable java.util.UUID id) {
        UUID marketId = securityUtils.getCurrentMarketId();
        return ResponseEntity.ok(referenceDataService.getFarmerById(id, marketId));
    }

    @GetMapping("/buyers")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<List<BuyerDTO>> getBuyers() {
        UUID marketId = securityUtils.getCurrentMarketId();
        return ResponseEntity.ok(referenceDataService.getBuyersByMarket(marketId));
    }

    @GetMapping("/buyers/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<BuyerDTO> getBuyer(@PathVariable java.util.UUID id) {
        UUID marketId = securityUtils.getCurrentMarketId();
        return ResponseEntity.ok(referenceDataService.getBuyerById(id, marketId));
    }

    @GetMapping("/commodities")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<List<CommodityDTO>> getCommodities() {
        return ResponseEntity.ok(referenceDataService.getAllCommodities());
    }

    @GetMapping("/asset-types")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<List<AssetTypeDTO>> getAssetTypes() {
        UUID marketId = securityUtils.getCurrentMarketId();
        return ResponseEntity.ok(referenceDataService.getAssetTypesByMarket(marketId));
    }
}

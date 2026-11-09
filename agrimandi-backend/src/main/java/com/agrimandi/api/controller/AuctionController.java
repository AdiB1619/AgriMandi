package com.agrimandi.api.controller;

import com.agrimandi.api.dto.BidRequestDTO;
import com.agrimandi.api.dto.BidResponseDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.dto.WinnerConfirmationDTO;
import com.agrimandi.api.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lots/{lotId}")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping("/auction/start")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<Void> startAuction(@PathVariable UUID lotId) {
        auctionService.startAuction(lotId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auction/close")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<Void> closeAuction(@PathVariable UUID lotId) {
        auctionService.closeAuction(lotId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bids")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'FARMER', 'BUYER')")
    public ResponseEntity<List<BidResponseDTO>> getBids(@PathVariable UUID lotId) {
        return ResponseEntity.ok(auctionService.getAuctionBids(lotId));
    }

    @PostMapping("/bids")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<BidResponseDTO> recordBid(@PathVariable UUID lotId, @Valid @RequestBody BidRequestDTO request) {
        return ResponseEntity.ok(auctionService.recordBid(lotId, request));
    }

    @GetMapping("/auction/highest-bids")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'FARMER', 'BUYER')")
    public ResponseEntity<List<BidResponseDTO>> getHighestBids(@PathVariable UUID lotId) {
        return ResponseEntity.ok(auctionService.getHighestBids(lotId));
    }

    @PostMapping("/auction/confirm-winner")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<TransactionResponseDTO> confirmWinner(@PathVariable UUID lotId, @Valid @RequestBody WinnerConfirmationDTO request) {
        return ResponseEntity.ok(auctionService.confirmWinner(lotId, request));
    }
}

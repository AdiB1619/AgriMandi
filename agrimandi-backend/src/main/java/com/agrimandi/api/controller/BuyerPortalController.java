package com.agrimandi.api.controller;

import com.agrimandi.api.dto.ReferenceDTOs.BuyerDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.security.CustomUserDetails;
import com.agrimandi.api.service.ReferenceDataService;
import com.agrimandi.api.service.TransactionService;
import com.agrimandi.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
public class BuyerPortalController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final ReferenceDataService referenceDataService;

    /**
     * The signed-in buyer's own record, scoped to the authenticated principal.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<BuyerDTO> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getBuyerId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No buyer profile is linked to this account");
        }
        return ResponseEntity.ok(referenceDataService.getBuyerById(
                userDetails.getBuyerId(), userDetails.getMarketId()));
    }

    @GetMapping("/me/purchases")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<TransactionResponseDTO>> getMyPurchases(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TransactionResponseDTO> transactions = transactionRepository.findByBuyerId(userDetails.getBuyerId())
                .stream()
                .map(transactionService::mapToDetailDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }
}

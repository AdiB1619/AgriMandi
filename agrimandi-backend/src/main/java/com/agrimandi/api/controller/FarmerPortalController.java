package com.agrimandi.api.controller;

import com.agrimandi.api.dto.ReferenceDTOs.FarmerDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.security.CustomUserDetails;
import com.agrimandi.api.service.ReferenceDataService;
import com.agrimandi.api.service.TransactionService;
import com.agrimandi.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/farmer")
@RequiredArgsConstructor
public class FarmerPortalController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final ReferenceDataService referenceDataService;

    /**
     * The signed-in farmer's own record. Scoped to the authenticated principal so a farmer
     * never has to (and never can) address another farmer's profile.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FarmerDTO> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getFarmerId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No farmer profile is linked to this account");
        }
        return ResponseEntity.ok(referenceDataService.getFarmerById(
                userDetails.getFarmerId(), userDetails.getMarketId()));
    }

    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<TransactionResponseDTO>> getMyTransactions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TransactionResponseDTO> transactions = transactionRepository.findByFarmerId(userDetails.getFarmerId())
                .stream()
                .map(transactionService::mapToDetailDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }
}

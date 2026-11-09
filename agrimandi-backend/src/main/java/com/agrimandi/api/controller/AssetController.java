package com.agrimandi.api.controller;

import com.agrimandi.api.dto.AssetIssueReturnDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.service.AssetService;
import com.agrimandi.api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions/{transactionId}/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final TransactionService transactionService;

    /**
     * Records an asset movement and returns the refreshed transaction.
     *
     * <p>Returning the {@code AssetTransaction} entity would drag its lazily-loaded transaction
     * and asset type into the response, which does not serialise into valid JSON. The caller wants
     * the recalculated issued/returned/outstanding position, which the DTO carries.
     */
    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<TransactionResponseDTO> issueAsset(
            @PathVariable UUID transactionId,
            @Valid @RequestBody AssetIssueReturnDTO request) {
        assetService.issueAsset(transactionId, request);
        return ResponseEntity.ok(transactionService.getTransactionDetail(transactionId));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<TransactionResponseDTO> returnAsset(
            @PathVariable UUID transactionId,
            @Valid @RequestBody AssetIssueReturnDTO request) {
        assetService.returnAsset(transactionId, request);
        return ResponseEntity.ok(transactionService.getTransactionDetail(transactionId));
    }
}

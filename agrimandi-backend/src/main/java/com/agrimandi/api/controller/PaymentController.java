package com.agrimandi.api.controller;

import com.agrimandi.api.dto.PaymentRequestDTO;
import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.service.PaymentService;
import com.agrimandi.api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions/{transactionId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionService transactionService;

    /**
     * Records a payment and returns the refreshed transaction.
     *
     * <p>The response is the transaction detail DTO rather than the {@code Payment} entity: the
     * entity graph reaches back into a lazily-loaded transaction, which does not serialise into
     * valid JSON, and the caller needs the recalculated balance anyway.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<TransactionResponseDTO> recordPayment(
            @PathVariable UUID transactionId,
            @Valid @RequestBody PaymentRequestDTO request) {
        paymentService.recordPayment(transactionId, request);
        return ResponseEntity.ok(transactionService.getTransactionDetail(transactionId));
    }

    @GetMapping("/pending-amount")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'FARMER', 'BUYER')")
    public ResponseEntity<Map<String, BigDecimal>> getPendingAmount(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(Map.of("pendingAmount", paymentService.getPendingAmount(transactionId)));
    }
}

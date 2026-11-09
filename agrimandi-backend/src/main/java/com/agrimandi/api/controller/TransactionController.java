package com.agrimandi.api.controller;

import com.agrimandi.api.dto.TransactionResponseDTO;
import com.agrimandi.api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'FARMER', 'BUYER')")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionDetail(id));
    }
}

package com.agrimandi.api.controller;

import com.agrimandi.api.dto.LotRequestDTO;
import com.agrimandi.api.entity.Lot;
import com.agrimandi.api.service.LotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<Lot> createLot(@Valid @RequestBody LotRequestDTO request) {
        return ResponseEntity.ok(lotService.createLot(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'FARMER', 'BUYER')")
    public ResponseEntity<java.util.List<Lot>> getAllLots() {
        return ResponseEntity.ok(lotService.getAllLots());
    }

    @GetMapping("/{lotId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'FARMER', 'BUYER')")
    public ResponseEntity<Lot> getLot(@PathVariable java.util.UUID lotId) {
        return ResponseEntity.ok(lotService.getLot(lotId));
    }

    @PostMapping("/{lotId}/weighment")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<com.agrimandi.api.entity.Weighment> recordWeighment(
            @PathVariable java.util.UUID lotId,
            @Valid @RequestBody com.agrimandi.api.dto.WeighmentRequestDTO request) {
        return ResponseEntity.ok(lotService.recordWeighment(lotId, request));
    }
}

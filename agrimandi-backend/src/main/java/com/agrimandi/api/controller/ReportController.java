package com.agrimandi.api.controller;

import com.agrimandi.api.dto.report.PageResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AssetLedgerRow;
import com.agrimandi.api.dto.report.ReportDTOs.AssetSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AuctionSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.BuyerSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.DateRange;
import com.agrimandi.api.dto.report.ReportDTOs.FarmerSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentLedgerRow;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.ReportPeriod;
import com.agrimandi.api.dto.report.ReportDTOs.TransactionSummaryResponse;
import com.agrimandi.api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Operational reporting for AGENT and ADMIN only.
 *
 * <p>FARMER and BUYER are rejected server-side. The market is always taken from the
 * authenticated principal, never from a request parameter.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/transactions")
    public ResponseEntity<TransactionSummaryResponse> transactionSummary(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.transactionSummary(range(period, from, to)));
    }

    @GetMapping("/payments")
    public ResponseEntity<PaymentSummaryResponse> paymentSummary(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.paymentSummary(range(period, from, to)));
    }

    @GetMapping("/assets")
    public ResponseEntity<AssetSummaryResponse> assetSummary(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.assetSummary(range(period, from, to)));
    }

    @GetMapping("/auctions")
    public ResponseEntity<AuctionSummaryResponse> auctionSummary(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.auctionSummary(range(period, from, to)));
    }

    @GetMapping("/buyers")
    public ResponseEntity<BuyerSummaryResponse> buyerSummary(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID buyerId) {
        return ResponseEntity.ok(reportService.buyerSummary(range(period, from, to), buyerId));
    }

    @GetMapping("/farmers")
    public ResponseEntity<FarmerSummaryResponse> farmerSummary(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID farmerId) {
        return ResponseEntity.ok(reportService.farmerSummary(range(period, from, to), farmerId));
    }

    @GetMapping("/payment-ledger")
    public ResponseEntity<PageResponse<PaymentLedgerRow>> paymentLedger(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID buyerId,
            @RequestParam(required = false) UUID farmerId,
            @RequestParam(required = false) UUID commodityId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.paymentLedger(
                range(period, from, to), status, buyerId, farmerId, commodityId, search, page, size));
    }

    @GetMapping("/asset-ledger")
    public ResponseEntity<PageResponse<AssetLedgerRow>> assetLedger(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID buyerId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.assetLedger(range(period, from, to), buyerId, search, page, size));
    }

    @GetMapping("/overview")
    public ResponseEntity<com.agrimandi.api.dto.report.ReportDTOs.AdminOversightResponse> adminOversight() {
        return ResponseEntity.ok(reportService.adminOversight());
    }

    private DateRange range(ReportPeriod period, LocalDate from, LocalDate to) {
        return reportService.resolveRange(period, from, to);
    }
}

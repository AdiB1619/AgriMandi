package com.agrimandi.api.controller;

import com.agrimandi.api.dto.report.AuditLogDTO;
import com.agrimandi.api.dto.report.PageResponse;
import com.agrimandi.api.dto.report.ReportDTOs.DateRange;
import com.agrimandi.api.dto.report.ReportDTOs.ReportPeriod;
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
import java.util.List;
import java.util.UUID;

/**
 * Read-only audit trail. Restricted to ADMIN because the existing authorization model does
 * not grant any other role visibility over the market-wide audit log. Every row is scoped to
 * the administrator's own market.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<PageResponse<AuditLogDTO>> search(
            @RequestParam(required = false) ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        DateRange range = reportService.resolveRange(period, from, to);
        return ResponseEntity.ok(reportService.auditTrail(
                range, actorId, action, entityType, entityId, page, size));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<String>> actions() {
        return ResponseEntity.ok(reportService.auditActions());
    }
}

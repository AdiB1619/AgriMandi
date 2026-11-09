package com.agrimandi.api.dto.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only reporting responses. Reporting never returns JPA entities: aggregates are
 * computed in the database and projected into these types.
 */
public class ReportDTOs {

    private ReportDTOs() {
    }

    // ── Period / filter inputs ────────────────────────────────────────────────

    public enum ReportPeriod {
        TODAY,
        YESTERDAY,
        CURRENT_WEEK,
        CURRENT_MONTH,
        CUSTOM
    }

    /** Half-open range [from, to) resolved server-side from the requested period. */
    public record DateRange(OffsetDateTime from, OffsetDateTime to) {
    }

    // ── Aggregates (JPQL constructor projections) ─────────────────────────────

    public record TransactionAggregate(
            Long totalTransactions,
            BigDecimal grossSaleValue,
            BigDecimal netSaleValue,
            BigDecimal totalQuantity,
            BigDecimal averageSaleRate) {
    }

    public record PaymentAggregate(
            Long totalTransactions,
            BigDecimal totalBilled,
            BigDecimal totalReceived,
            BigDecimal totalOutstanding,
            Long partiallyPaidCount,
            Long settledCount) {
    }

    public record AssetAggregate(
            Long assetRecords,
            Long totalIssued,
            Long totalReturned,
            Long totalOutstanding) {
    }

    public record AuctionAggregate(
            Long totalLots,
            Long totalAuctions,
            Long completedAuctions,
            Long activeAuctions,
            Long tieCases,
            Long transactionsGenerated,
            BigDecimal totalQuantityAuctioned) {
    }

    public record CountAggregate(Long total) {
    }

    public record AmountAggregate(BigDecimal total) {
    }

    public record StatusCount(String status, Long count) {
    }

    public record BuyerActivity(
            UUID buyerId,
            String buyerName,
            String businessName,
            Long purchases,
            BigDecimal totalPurchaseValue,
            BigDecimal totalQuantity,
            BigDecimal outstandingPayment,
            Long outstandingAssetRecords) {
    }

    public record FarmerActivity(
            UUID farmerId,
            String farmerName,
            Long lots,
            BigDecimal totalQuantity,
            Long transactions,
            BigDecimal totalTransactionValue,
            BigDecimal outstandingPayment) {
    }

    // ── Transaction / payment summary responses ───────────────────────────────

    public record TransactionSummaryResponse(
            DateRange range,
            String marketId,
            TransactionAggregate aggregate,
            List<StatusCount> statusBreakdown,
            BigDecimal totalOutstandingPayment) {
    }

    public record PaymentSummaryResponse(
            DateRange range,
            String marketId,
            PaymentAggregate aggregate,
            List<StatusCount> statusBreakdown) {
    }

    public record AssetSummaryResponse(
            DateRange range,
            String marketId,
            AssetAggregate aggregate) {
    }

    public record AuctionSummaryResponse(
            DateRange range,
            String marketId,
            AuctionAggregate aggregate) {
    }

    public record BuyerSummaryResponse(
            DateRange range,
            String marketId,
            List<BuyerActivity> buyers) {
    }

    public record FarmerSummaryResponse(
            DateRange range,
            String marketId,
            List<FarmerActivity> farmers) {
    }

    // ── Ledger rows ───────────────────────────────────────────────────────────

    public record PaymentLedgerRow(
            UUID transactionId,
            String transactionNumber,
            OffsetDateTime transactionDate,
            UUID lotId,
            String lotNumber,
            String commodity,
            UUID farmerId,
            String farmerName,
            UUID buyerId,
            String buyerName,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            String paymentStatus,
            OffsetDateTime latestPaymentDate) {
    }

    public record AssetLedgerRow(
            UUID transactionId,
            String transactionNumber,
            String lotNumber,
            UUID assetTypeId,
            String assetTypeName,
            UUID buyerId,
            String buyerName,
            Integer quantityIssued,
            Integer quantityReturned,
            Integer quantityOutstanding,
            OffsetDateTime issueDate,
            OffsetDateTime latestReturnDate,
            String assetStatus) {
    }

    // ── Admin oversight ───────────────────────────────────────────────────────

    public record ExceptionCount(String label, Long count) {
    }

    public record AdminOversightResponse(
            String marketId,
            OffsetDateTime generatedAt,
            CountAggregate todayLots,
            AmountAggregate todayAuctionValue,
            CountAggregate todayTransactions,
            AmountAggregate todayPaymentCollection,
            AmountAggregate totalOutstandingPayments,
            AssetAggregate outstandingAssets,
            CountAggregate activeAuctions,
            List<PaymentLedgerRow> recentTransactions,
            List<AuditActivityDTO> recentAuditActivity,
            List<BuyerActivity> topBuyersByValue,
            List<FarmerActivity> farmerActivity,
            List<ExceptionCount> exceptions) {
    }

    /** Compact audit projection used by the admin oversight dashboard. */
    public record AuditActivityDTO(
            UUID id,
            String action,
            String entityType,
            UUID entityId,
            String actorUsername,
            String actorRole,
            OffsetDateTime timestamp) {
    }
}

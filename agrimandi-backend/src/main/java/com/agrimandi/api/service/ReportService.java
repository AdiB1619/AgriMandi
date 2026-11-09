package com.agrimandi.api.service;

import com.agrimandi.api.dto.report.AuditLogDTO;
import com.agrimandi.api.dto.report.PageResponse;
import com.agrimandi.api.dto.report.ReportProjections.AssetAggregateView;
import com.agrimandi.api.dto.report.ReportProjections.AuctionAggregateView;
import com.agrimandi.api.dto.report.ReportProjections.BuyerActivityView;
import com.agrimandi.api.dto.report.ReportProjections.FarmerActivityView;
import com.agrimandi.api.dto.report.ReportProjections.IdCountView;
import com.agrimandi.api.dto.report.ReportProjections.OwnerAmountView;
import com.agrimandi.api.dto.report.ReportProjections.TransactionAggregateView;
import com.agrimandi.api.dto.report.ReportDTOs.AdminOversightResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AmountAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.AssetAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.AssetLedgerRow;
import com.agrimandi.api.dto.report.ReportDTOs.AssetSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AuctionAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.AuctionSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.AuditActivityDTO;
import com.agrimandi.api.dto.report.ReportDTOs.BuyerActivity;
import com.agrimandi.api.dto.report.ReportDTOs.BuyerSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.CountAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.DateRange;
import com.agrimandi.api.dto.report.ReportDTOs.ExceptionCount;
import com.agrimandi.api.dto.report.ReportDTOs.FarmerActivity;
import com.agrimandi.api.dto.report.ReportDTOs.FarmerSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentLedgerRow;
import com.agrimandi.api.dto.report.ReportDTOs.PaymentSummaryResponse;
import com.agrimandi.api.dto.report.ReportDTOs.ReportPeriod;
import com.agrimandi.api.dto.report.ReportDTOs.StatusCount;
import com.agrimandi.api.dto.report.ReportDTOs.TransactionAggregate;
import com.agrimandi.api.dto.report.ReportDTOs.TransactionSummaryResponse;
import com.agrimandi.api.entity.AuditLog;
import com.agrimandi.api.entity.Transaction;
import com.agrimandi.api.exception.BusinessValidationException;
import com.agrimandi.api.repository.AssetTransactionRepository;
import com.agrimandi.api.repository.AuditLogRepository;
import com.agrimandi.api.repository.AuctionRepository;
import com.agrimandi.api.repository.LotRepository;
import com.agrimandi.api.repository.PaymentRepository;
import com.agrimandi.api.repository.TransactionRepository;
import com.agrimandi.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Operational reporting and APMC oversight.
 *
 * <p>Every query is scoped to the market of the authenticated user, which is taken from the
 * server-side principal only. No market, farmer or buyer identifier supplied by the caller is
 * trusted for scoping; the optional {@code buyerId}/{@code farmerId} parameters are only ever
 * applied <em>on top of</em> the authenticated market.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final AssetTransactionRepository assetTransactionRepository;
    private final LotRepository lotRepository;
    private final AuctionRepository auctionRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    // ── Period handling ──────────────────────────────────────────────────────

    /**
     * Resolves the requested period into a half-open [from, to) range on the server so that
     * filtering never depends on the client clock or on client-side row filtering.
     *
     * <p>An explicit {@code from}/{@code to} pair is always honoured, even when the caller did not
     * also send {@code period=CUSTOM}. Silently falling back to the current month in that case
     * would report on a period the caller never asked for.
     */
    public DateRange resolveRange(ReportPeriod period, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate start;
        LocalDate endExclusive;

        ReportPeriod effective = period;
        if (effective == null && (from != null || to != null)) {
            effective = ReportPeriod.CUSTOM;
        }
        if (effective == null) {
            effective = ReportPeriod.CURRENT_MONTH;
        }

        switch (effective) {
            case TODAY -> {
                start = today;
                endExclusive = today.plusDays(1);
            }
            case YESTERDAY -> {
                start = today.minusDays(1);
                endExclusive = today;
            }
            case CURRENT_WEEK -> {
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                endExclusive = start.plusWeeks(1);
            }
            case CURRENT_MONTH -> {
                start = today.withDayOfMonth(1);
                endExclusive = start.plusMonths(1);
            }
            case CUSTOM -> {
                if (from == null || to == null) {
                    throw new BusinessValidationException("A custom date range requires both 'from' and 'to' dates");
                }
                if (to.isBefore(from)) {
                    throw new BusinessValidationException("'to' date must not be before 'from' date");
                }
                start = from;
                endExclusive = to.plusDays(1);
            }
            default -> throw new BusinessValidationException("Unsupported reporting period");
        }

        return new DateRange(start.atStartOfDay(zone).toOffsetDateTime(),
                endExclusive.atStartOfDay(zone).toOffsetDateTime());
    }

    private UUID currentMarketId() {
        String role = securityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"AGENT".equals(role)) {
            throw new BusinessValidationException("Market-wide reporting is not available for your role");
        }
        UUID marketId = securityUtils.getCurrentMarketId();
        if (marketId == null) {
            throw new BusinessValidationException("User is not associated with a market");
        }
        return marketId;
    }

    // ── A. Transaction summary ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionSummaryResponse transactionSummary(DateRange range) {
        UUID marketId = currentMarketId();
        TransactionAggregate aggregate = toAggregate(
                transactionRepository.summarizeForMarket(marketId, range.from(), range.to()));
        List<StatusCount> breakdown = statusBreakdown(marketId, range);
        BigDecimal outstanding = transactionRepository.sumBilledForPeriod(marketId, range.from(), range.to())
                .subtract(transactionRepository.sumReceivedAgainstPeriodTransactions(marketId, range.from(), range.to()));
        return new TransactionSummaryResponse(range, marketId.toString(), aggregate, breakdown, nonNegative(outstanding));
    }

    // ── B. Payment summary ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentSummaryResponse paymentSummary(DateRange range) {
        UUID marketId = currentMarketId();

        long totalTransactions = Optional2.orZero(
                transactionRepository.summarizeForMarket(marketId, range.from(), range.to()).getTotalTransactions());
        BigDecimal totalBilled = transactionRepository.sumBilledForPeriod(marketId, range.from(), range.to());
        BigDecimal totalReceived = transactionRepository.sumReceivedInPeriod(marketId, range.from(), range.to());
        BigDecimal outstanding = totalBilled
                .subtract(transactionRepository.sumReceivedAgainstPeriodTransactions(marketId, range.from(), range.to()));

        Long partiallyPaid = Optional2.orZero(
                transactionRepository.countByStatus(marketId, range.from(), range.to(), "PARTIALLY_PAID").getTotal());
        Long settled = Optional2.orZero(
                transactionRepository.countByStatus(marketId, range.from(), range.to(), "PAID").getTotal());

        PaymentAggregate aggregate = new PaymentAggregate(
                totalTransactions, totalBilled, totalReceived, nonNegative(outstanding), partiallyPaid, settled);

        return new PaymentSummaryResponse(range, marketId.toString(), aggregate, statusBreakdown(marketId, range));
    }

    // ── C. Asset summary ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AssetSummaryResponse assetSummary(DateRange range) {
        UUID marketId = currentMarketId();
        AssetAggregate aggregate = toAssetAggregate(
                assetTransactionRepository.summarizeForMarket(marketId, range.from(), range.to()));
        return new AssetSummaryResponse(range, marketId.toString(), aggregate);
    }

    // ── D. Auction summary ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuctionSummaryResponse auctionSummary(DateRange range) {
        UUID marketId = currentMarketId();
        AuctionAggregateView period = auctionRepository.summarizeForPeriod(marketId, range.from(), range.to());

        long lots = Optional2.orZero(lotRepository.countForPeriod(marketId, range.from(), range.to()).getTotal());
        long transactions = Optional2.orZero(
                transactionRepository.summarizeForMarket(marketId, range.from(), range.to()).getTotalTransactions());
        long activeAuctions = Optional2.orZero(auctionRepository.countActiveForMarket(marketId).getTotal());

        AuctionAggregate aggregate = new AuctionAggregate(
                lots,
                Optional2.orZero(period.getTotalAuctions()),
                Optional2.orZero(period.getCompletedAuctions()),
                activeAuctions,
                Optional2.orZero(period.getTieCases()),
                transactions,
                period.getTotalQuantityAuctioned() != null ? period.getTotalQuantityAuctioned() : BigDecimal.ZERO);

        return new AuctionSummaryResponse(range, marketId.toString(), aggregate);
    }

    // ── E. Buyer summary ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BuyerSummaryResponse buyerSummary(DateRange range, UUID buyerId) {
        UUID marketId = currentMarketId();
        List<BuyerActivityView> activity = transactionRepository.buyerActivity(marketId, range.from(), range.to(), buyerId);

        Map<UUID, BigDecimal> received = amountByOwner(
                transactionRepository.receivedByBuyerForPeriod(marketId, range.from(), range.to()));
        Map<UUID, Long> outstandingAssets = countByOwner(
                transactionRepository.outstandingAssetRecordsByBuyer(marketId, range.from(), range.to()));

        List<BuyerActivity> enriched = activity.stream()
                .map(row -> new BuyerActivity(
                        row.getBuyerId(),
                        row.getBuyerName(),
                        row.getBusinessName(),
                        Optional2.orZero(row.getPurchases()),
                        row.getTotalPurchaseValue(),
                        row.getTotalQuantity(),
                        nonNegative(row.getTotalPurchaseValue()
                                .subtract(received.getOrDefault(row.getBuyerId(), BigDecimal.ZERO))),
                        outstandingAssets.getOrDefault(row.getBuyerId(), 0L)))
                .toList();

        return new BuyerSummaryResponse(range, marketId.toString(), enriched);
    }

    // ── F. Farmer summary ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FarmerSummaryResponse farmerSummary(DateRange range, UUID farmerId) {
        UUID marketId = currentMarketId();
        List<FarmerActivityView> activity = transactionRepository.farmerActivity(marketId, range.from(), range.to(), farmerId);

        Map<UUID, BigDecimal> received = amountByOwner(
                transactionRepository.receivedByFarmerForPeriod(marketId, range.from(), range.to()));
        Map<UUID, Long> lotCounts = countByOwner(
                transactionRepository.lotActivityByFarmer(marketId, range.from(), range.to()));

        List<FarmerActivity> enriched = activity.stream()
                .map(row -> new FarmerActivity(
                        row.getFarmerId(),
                        row.getFarmerName(),
                        lotCounts.getOrDefault(row.getFarmerId(), 0L),
                        row.getTotalQuantity(),
                        Optional2.orZero(row.getTransactions()),
                        row.getTotalTransactionValue(),
                        nonNegative(row.getTotalTransactionValue()
                                .subtract(received.getOrDefault(row.getFarmerId(), BigDecimal.ZERO)))))
                .toList();

        return new FarmerSummaryResponse(range, marketId.toString(), enriched);
    }

    // ── G. Payment ledger (paginated) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<PaymentLedgerRow> paymentLedger(DateRange range, String status, UUID buyerId,
                                                        UUID farmerId, UUID commodityId, String search,
                                                        int page, int size) {
        UUID marketId = currentMarketId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "transactionDate"));

        Page<Transaction> transactions = transactionRepository.findLedgerPage(
                marketId, range.from(), range.to(), blankToNull(status), buyerId, farmerId, commodityId,
                blankToNull(search) == null ? null : "%" + search.trim().toLowerCase() + "%", pageable);

        Map<UUID, PaymentRollup> rollups = rollupsFor(
                transactions.getContent().stream().map(Transaction::getId).toList());

        return PageResponse.of(transactions.map(tx -> toPaymentLedgerRow(tx, rollups.get(tx.getId()))));
    }

    private PaymentLedgerRow toPaymentLedgerRow(Transaction tx, PaymentRollup rollup) {
        BigDecimal paid = rollup != null ? rollup.paid() : BigDecimal.ZERO;
        return new PaymentLedgerRow(
                tx.getId(),
                tx.getTransactionNumber(),
                tx.getTransactionDate(),
                tx.getLot().getId(),
                tx.getLot().getLotNumber(),
                tx.getLot().getCommodity().getName(),
                tx.getFarmer().getId(),
                tx.getFarmer().getName(),
                tx.getBuyer().getId(),
                tx.getBuyer().getName(),
                tx.getNetAmount(),
                paid,
                nonNegative(tx.getNetAmount().subtract(paid)),
                tx.getStatus(),
                rollup != null ? rollup.latestPaymentDate() : null);
    }

    // ── H. Asset ledger (paginated) ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AssetLedgerRow> assetLedger(DateRange range, UUID buyerId, String search,
                                                    int page, int size) {
        UUID marketId = currentMarketId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "transactionDate"));

        Page<Transaction> transactions = transactionRepository.findTransactionsWithAssets(
                marketId, range.from(), range.to(), pageable);

        List<UUID> transactionIds = transactions.getContent().stream().map(Transaction::getId).toList();
        if (transactionIds.isEmpty()) {
            return new PageResponse<>(List.of(), transactions.getNumber(), transactions.getSize(),
                    transactions.getTotalElements(), transactions.getTotalPages(),
                    transactions.isFirst(), transactions.isLast());
        }

        Map<UUID, List<AssetRollup>> rollups = new HashMap<>();
        for (Object[] row : assetTransactionRepository.rollupByTransactionIds(transactionIds)) {
            UUID transactionId = (UUID) row[0];
            rollups.computeIfAbsent(transactionId, k -> new ArrayList<>())
                    .add(new AssetRollup((UUID) row[1], (String) row[2],
                            ((Number) row[3]).intValue(), ((Number) row[4]).intValue(),
                            (OffsetDateTime) row[5], (OffsetDateTime) row[6]));
        }

        String searchTerm = blankToNull(search) == null ? null : search.trim().toLowerCase();

        List<AssetLedgerRow> rows = new ArrayList<>();
        for (Transaction tx : transactions.getContent()) {
            for (AssetRollup rollup : rollups.getOrDefault(tx.getId(), List.of())) {
                if (buyerId != null && !buyerId.equals(tx.getBuyer().getId())) {
                    continue;
                }
                if (searchTerm != null && !matchesSearch(searchTerm, tx, rollup)) {
                    continue;
                }
                int outstanding = rollup.issued() - rollup.returned();
                rows.add(new AssetLedgerRow(
                        tx.getId(),
                        tx.getTransactionNumber(),
                        tx.getLot().getLotNumber(),
                        rollup.assetTypeId(),
                        rollup.assetTypeName(),
                        tx.getBuyer().getId(),
                        tx.getBuyer().getName(),
                        rollup.issued(),
                        rollup.returned(),
                        outstanding,
                        rollup.issueDate(),
                        rollup.latestReturnDate(),
                        deriveAssetStatus(rollup.issued(), rollup.returned())));
            }
        }

        return new PageResponse<>(rows, transactions.getNumber(), transactions.getSize(),
                transactions.getTotalElements(), transactions.getTotalPages(),
                transactions.isFirst(), transactions.isLast());
    }

    private boolean matchesSearch(String term, Transaction tx, AssetRollup rollup) {
        return contains(tx.getTransactionNumber(), term)
                || contains(tx.getLot().getLotNumber(), term)
                || contains(tx.getBuyer().getName(), term)
                || contains(rollup.assetTypeName(), term);
    }

    /**
     * Asset status is derived from the persisted movement totals. The model stores individual
     * ISSUED / RETURNED movements and has no due-date or grace-period column, so an OVERDUE
     * state is deliberately not invented here.
     */
    private String deriveAssetStatus(int issued, int returned) {
        if (issued <= 0) {
            return "NO_ISSUE_RECORDED";
        }
        if (returned <= 0) {
            return "OUTSTANDING";
        }
        if (returned >= issued) {
            return "RETURNED";
        }
        return "PARTIALLY_RETURNED";
    }

    // ── I. Admin / APMC oversight ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminOversightResponse adminOversight() {
        UUID marketId = currentMarketId();
        ZoneId zone = ZoneId.systemDefault();
        DateRange today = new DateRange(
                LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime(),
                LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toOffsetDateTime());

        CountAggregate todayLots = new CountAggregate(Optional2.orZero(
                lotRepository.countForPeriod(marketId, today.from(), today.to()).getTotal()));

        CountAggregate todayTransactions = new CountAggregate(Optional2.orZero(
                transactionRepository.summarizeForMarket(marketId, today.from(), today.to()).getTotalTransactions()));

        AmountAggregate todayAuctionValue = new AmountAggregate(
                transactionRepository.sumBilledForPeriod(marketId, today.from(), today.to()));
        AmountAggregate todayCollection = new AmountAggregate(
                transactionRepository.sumReceivedInPeriod(marketId, today.from(), today.to()));

        BigDecimal allTimeOutstanding = transactionRepository.sumBilledForPeriod(marketId, MIN_DATE, MAX_DATE)
                .subtract(transactionRepository.sumReceivedAgainstPeriodTransactions(marketId, MIN_DATE, MAX_DATE));
        AmountAggregate outstandingPayments = new AmountAggregate(nonNegative(allTimeOutstanding));

        AssetAggregate outstandingAssets = toAssetAggregate(
                assetTransactionRepository.outstandingForMarket(marketId));
        CountAggregate activeAuctions = new CountAggregate(Optional2.orZero(
                auctionRepository.countActiveForMarket(marketId).getTotal()));

        List<PaymentLedgerRow> recentTransactions = transactionRepository
                .findRecentForMarket(marketId, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(tx -> toPaymentLedgerRow(tx, rollupFor(tx.getId())))
                .toList();

        List<AuditActivityDTO> recentAudit = auditLogRepository
                .findRecentForMarket(marketId, PageRequest.of(0, 8))
                .stream()
                .map(this::toAuditActivity)
                .toList();

        DateRange currentMonth = resolveRange(ReportPeriod.CURRENT_MONTH, null, null);
        List<BuyerActivity> topBuyers = buyerSummary(currentMonth, null).buyers();
        List<FarmerActivity> farmerActivity = farmerSummary(currentMonth, null).farmers();

        List<ExceptionCount> exceptions = List.of(
                new ExceptionCount("Transactions pending payment", countStatus(marketId, "CREATED")),
                new ExceptionCount("Transactions partially paid", countStatus(marketId, "PARTIALLY_PAID")),
                new ExceptionCount("Auctions awaiting winner confirmation", countStatus(marketId, "WINNER_PENDING_CONFIRMATION")),
                new ExceptionCount("Auctions requiring tie decision", countStatus(marketId, "TIE_REQUIRES_AGENT_DECISION")),
                new ExceptionCount("Lots pending weighment", Optional2.orZero(
                        lotRepository.countByMarketAndStatus(marketId, "PENDING_WEIGHMENT").getTotal())),
                new ExceptionCount("Returnable assets outstanding", outstandingAssets.totalOutstanding()));

        return new AdminOversightResponse(
                marketId.toString(),
                OffsetDateTime.now(),
                todayLots,
                todayAuctionValue,
                todayTransactions,
                todayCollection,
                outstandingPayments,
                outstandingAssets,
                activeAuctions,
                recentTransactions,
                recentAudit,
                topBuyers,
                farmerActivity,
                exceptions);
    }

    private long countStatus(UUID marketId, String status) {
        return Optional2.orZero(
                transactionRepository.countByStatus(marketId, MIN_DATE, MAX_DATE, status).getTotal());
    }

    // ── J. Audit trail ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> auditTrail(DateRange range, UUID actorId, String action,
                                                String entityType, UUID entityId,
                                                int page, int size) {
        UUID marketId = currentMarketId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogRepository.searchForMarket(
                marketId, range.from(), range.to(), actorId, blankToNull(action),
                blankToNull(entityType), entityId, pageable);
        return PageResponse.of(logs.map(this::toAuditLogDTO));
    }

    @Transactional(readOnly = true)
    public List<String> auditActions() {
        return auditLogRepository.findDistinctActionsForMarket(currentMarketId());
    }

    public AuditLogDTO toAuditLogDTO(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setActorUsername(log.getActor() != null ? log.getActor().getUsername() : null);
        dto.setActorRole(log.getActorRole());
        dto.setActorId(log.getActor() != null ? log.getActor().getId() : null);
        dto.setMarketId(log.getMarket() != null ? log.getMarket().getId() : null);
        dto.setMarketCode(log.getMarket() != null ? log.getMarket().getCode() : null);
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }

    private AuditActivityDTO toAuditActivity(AuditLog log) {
        return new AuditActivityDTO(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getActor() != null ? log.getActor().getUsername() : null,
                log.getActorRole(),
                log.getTimestamp());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static final OffsetDateTime MIN_DATE = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime MAX_DATE = OffsetDateTime.parse("2999-12-31T00:00:00Z");

    private record PaymentRollup(BigDecimal paid, OffsetDateTime latestPaymentDate) {
    }

    private record AssetRollup(UUID assetTypeId, String assetTypeName, int issued, int returned,
                               OffsetDateTime issueDate, OffsetDateTime latestReturnDate) {
    }

    private Map<UUID, PaymentRollup> rollupsFor(List<UUID> transactionIds) {
        Map<UUID, PaymentRollup> result = new HashMap<>();
        if (transactionIds.isEmpty()) {
            return result;
        }
        for (Object[] row : paymentRepository.rollupByTransactionIds(transactionIds)) {
            result.put((UUID) row[0], new PaymentRollup((BigDecimal) row[1], (OffsetDateTime) row[3]));
        }
        return result;
    }

    private PaymentRollup rollupFor(UUID transactionId) {
        return rollupsFor(List.of(transactionId)).get(transactionId);
    }

    private static <K, V> Map<K, V> indexById(List<Object[]> rows) {
        Map<K, V> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((K) row[0], (V) row[1]);
        }
        return map;
    }

    private List<StatusCount> statusBreakdown(UUID marketId, DateRange range) {
        return transactionRepository.statusBreakdown(marketId, range.from(), range.to()).stream()
                .map(view -> new StatusCount(view.getStatus(), Optional2.orZero(view.getCount())))
                .toList();
    }

    private static TransactionAggregate toAggregate(TransactionAggregateView view) {
        // Averaging winning rates across mixed price units (KG vs CRATE) would produce a number
        // that means nothing, so it is reported as absent instead of misleading.
        boolean singlePriceUnit = Optional2.orZero(view.getDistinctPriceUnits()) <= 1;
        return new TransactionAggregate(
                Optional2.orZero(view.getTotalTransactions()),
                zeroIfNull(view.getGrossSaleValue()),
                zeroIfNull(view.getNetSaleValue()),
                zeroIfNull(view.getTotalQuantity()),
                singlePriceUnit ? zeroIfNull(view.getAverageSaleRate()) : null);
    }

    private static AssetAggregate toAssetAggregate(AssetAggregateView view) {
        return new AssetAggregate(
                Optional2.orZero(view.getAssetRecords()),
                Optional2.orZero(view.getTotalIssued()),
                Optional2.orZero(view.getTotalReturned()),
                Optional2.orZero(view.getTotalOutstanding()));
    }

    private static Map<UUID, BigDecimal> amountByOwner(List<OwnerAmountView> views) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        views.forEach(view -> map.put(view.getOwnerId(), zeroIfNull(view.getTotal())));
        return map;
    }

    private static Map<UUID, Long> countByOwner(List<IdCountView> views) {
        Map<UUID, Long> map = new HashMap<>();
        views.forEach(view -> map.put(view.getOwnerId(), Optional2.orZero(view.getTotal())));
        return map;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean contains(String value, String lowerTerm) {
        return value != null && value.toLowerCase().contains(lowerTerm);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }

    /** Small null-safe unwrapping helper for aggregate projections. */
    private static final class Optional2 {
        private Optional2() {
        }

        static long orZero(Long value) {
            return value == null ? 0L : value;
        }
    }
}

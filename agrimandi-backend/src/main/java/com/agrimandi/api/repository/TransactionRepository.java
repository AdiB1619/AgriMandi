package com.agrimandi.api.repository;

import com.agrimandi.api.dto.report.ReportProjections.BuyerActivityView;
import com.agrimandi.api.dto.report.ReportProjections.CountView;
import com.agrimandi.api.dto.report.ReportProjections.FarmerActivityView;
import com.agrimandi.api.dto.report.ReportProjections.StatusCountView;
import com.agrimandi.api.dto.report.ReportProjections.TransactionAggregateView;
import com.agrimandi.api.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByStatus(String status, Pageable pageable);
    Optional<Transaction> findByLotId(UUID lotId);
    Optional<Transaction> findByAuctionId(UUID auctionId);

    java.util.List<Transaction> findByFarmerId(UUID farmerId);
    java.util.List<Transaction> findByBuyerId(UUID buyerId);

    // ── Reporting aggregates (computed in the database) ──────────────────────

    @Query("""
            SELECT COUNT(t) AS totalTransactions,
                   COALESCE(SUM(t.grossAmount), 0) AS grossSaleValue,
                   COALESCE(SUM(t.netAmount), 0) AS netSaleValue,
                   COALESCE(SUM(t.quantity), 0) AS totalQuantity,
                   COALESCE(AVG(t.winningRate), 0) AS averageSaleRate,
                   COUNT(DISTINCT t.priceUnit) AS distinctPriceUnits
            FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
            """)
    TransactionAggregateView summarizeForMarket(@Param("marketId") UUID marketId,
                                                @Param("from") OffsetDateTime from,
                                                @Param("to") OffsetDateTime to);

    @Query("""
            SELECT t.status AS status, COUNT(t) AS count
            FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
            GROUP BY t.status
            ORDER BY t.status
            """)
    List<StatusCountView> statusBreakdown(@Param("marketId") UUID marketId,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    /** Total billed value of the transactions created inside the period. */
    @Query("""
            SELECT COALESCE(SUM(t.netAmount), 0)
            FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
            """)
    BigDecimal sumBilledForPeriod(@Param("marketId") UUID marketId,
                                  @Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to);

    /**
     * Everything received against the transactions created inside the period, regardless of
     * when the payment itself was recorded. Billed minus this is the true outstanding balance.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.transaction.id IN (
                SELECT t.id FROM Transaction t
                WHERE t.lot.market.id = :marketId
                  AND t.transactionDate >= :from
                  AND t.transactionDate < :to)
            """)
    BigDecimal sumReceivedAgainstPeriodTransactions(@Param("marketId") UUID marketId,
                                                    @Param("from") OffsetDateTime from,
                                                    @Param("to") OffsetDateTime to);

    /** Payments actually collected inside the period (payment date based). */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.transaction.lot.market.id = :marketId
              AND p.paymentDate >= :from
              AND p.paymentDate < :to
            """)
    BigDecimal sumReceivedInPeriod(@Param("marketId") UUID marketId,
                                   @Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to);

    @Query("""
            SELECT COUNT(t) AS total
            FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND t.status = :status
            """)
    CountView countByStatus(@Param("marketId") UUID marketId,
                            @Param("from") OffsetDateTime from,
                            @Param("to") OffsetDateTime to,
                            @Param("status") String status);

    // ── Buyer / farmer activity ──────────────────────────────────────────────

    @Query("""
            SELECT t.buyer.id AS buyerId,
                   t.buyer.name AS buyerName,
                   t.buyer.businessName AS businessName,
                   COUNT(t) AS purchases,
                   COALESCE(SUM(t.netAmount), 0) AS totalPurchaseValue,
                   COALESCE(SUM(t.quantity), 0) AS totalQuantity
            FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND (:buyerId IS NULL OR t.buyer.id = :buyerId)
            GROUP BY t.buyer.id, t.buyer.name, t.buyer.businessName
            ORDER BY SUM(t.netAmount) DESC
            """)
    List<BuyerActivityView> buyerActivity(@Param("marketId") UUID marketId,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to,
                                          @Param("buyerId") UUID buyerId);

    @Query("""
            SELECT p.transaction.buyer.id AS ownerId, COALESCE(SUM(p.amount), 0) AS total
            FROM Payment p
            WHERE p.transaction.id IN (
                SELECT t.id FROM Transaction t
                WHERE t.lot.market.id = :marketId
                  AND t.transactionDate >= :from
                  AND t.transactionDate < :to)
            GROUP BY p.transaction.buyer.id
            """)
    List<com.agrimandi.api.dto.report.ReportProjections.OwnerAmountView> receivedByBuyerForPeriod(
            @Param("marketId") UUID marketId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("""
            SELECT at.transaction.buyer.id AS ownerId, COUNT(at) AS total
            FROM AssetTransaction at
            WHERE at.transaction.lot.market.id = :marketId
              AND at.transaction.id IN (
                SELECT t2.id FROM Transaction t2
                WHERE t2.lot.market.id = :marketId
                  AND t2.transactionDate >= :from
                  AND t2.transactionDate < :to)
              AND at.movementType = 'ISSUED'
              AND at.quantity > (SELECT COALESCE(SUM(a2.quantity), 0)
                                 FROM AssetTransaction a2
                                 WHERE a2.transaction = at.transaction
                                   AND a2.assetType = at.assetType
                                   AND a2.movementType = 'RETURNED')
            GROUP BY at.transaction.buyer.id
            """)
    List<com.agrimandi.api.dto.report.ReportProjections.IdCountView> outstandingAssetRecordsByBuyer(
            @Param("marketId") UUID marketId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("""
            SELECT t.farmer.id AS farmerId,
                   t.farmer.name AS farmerName,
                   COALESCE(SUM(t.quantity), 0) AS totalQuantity,
                   COUNT(t) AS transactions,
                   COALESCE(SUM(t.netAmount), 0) AS totalTransactionValue
            FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND (:farmerId IS NULL OR t.farmer.id = :farmerId)
            GROUP BY t.farmer.id, t.farmer.name
            ORDER BY SUM(t.netAmount) DESC
            """)
    List<FarmerActivityView> farmerActivity(@Param("marketId") UUID marketId,
                                            @Param("from") OffsetDateTime from,
                                            @Param("to") OffsetDateTime to,
                                            @Param("farmerId") UUID farmerId);

    @Query("""
            SELECT p.transaction.farmer.id AS ownerId, COALESCE(SUM(p.amount), 0) AS total
            FROM Payment p
            WHERE p.transaction.id IN (
                SELECT t.id FROM Transaction t
                WHERE t.lot.market.id = :marketId
                  AND t.transactionDate >= :from
                  AND t.transactionDate < :to)
            GROUP BY p.transaction.farmer.id
            """)
    List<com.agrimandi.api.dto.report.ReportProjections.OwnerAmountView> receivedByFarmerForPeriod(
            @Param("marketId") UUID marketId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("""
            SELECT l.farmer.id AS ownerId, COUNT(l) AS total
            FROM Lot l
            WHERE l.market.id = :marketId
              AND l.createdAt >= :from
              AND l.createdAt < :to
            GROUP BY l.farmer.id
            """)
    List<com.agrimandi.api.dto.report.ReportProjections.IdCountView> lotActivityByFarmer(
            @Param("marketId") UUID marketId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    // ── Paginated ledgers ────────────────────────────────────────────────────

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND (:status IS NULL OR t.status = :status)
              AND (:buyerId IS NULL OR t.buyer.id = :buyerId)
              AND (:farmerId IS NULL OR t.farmer.id = :farmerId)
              AND (:commodityId IS NULL OR t.lot.commodity.id = :commodityId)
              AND (:search IS NULL OR LOWER(t.transactionNumber) LIKE :search
                   OR LOWER(t.farmer.name) LIKE :search
                   OR LOWER(t.buyer.name) LIKE :search
                   OR LOWER(t.lot.lotNumber) LIKE :search)
            """)
    Page<Transaction> findLedgerPage(@Param("marketId") UUID marketId,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to,
                                     @Param("status") String status,
                                     @Param("buyerId") UUID buyerId,
                                     @Param("farmerId") UUID farmerId,
                                     @Param("commodityId") UUID commodityId,
                                     @Param("search") String search,
                                     Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.lot.market.id = :marketId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND EXISTS (SELECT 1 FROM AssetTransaction a WHERE a.transaction = t)
            """)
    Page<Transaction> findTransactionsWithAssets(@Param("marketId") UUID marketId,
                                                 @Param("from") OffsetDateTime from,
                                                 @Param("to") OffsetDateTime to,
                                                 Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.lot.market.id = :marketId
            ORDER BY t.transactionDate DESC
            """)
    Page<Transaction> findRecentForMarket(@Param("marketId") UUID marketId, Pageable pageable);
}

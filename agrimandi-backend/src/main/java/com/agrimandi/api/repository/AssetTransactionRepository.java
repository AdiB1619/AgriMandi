package com.agrimandi.api.repository;

import com.agrimandi.api.dto.report.ReportProjections.AssetAggregateView;
import com.agrimandi.api.entity.AssetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AssetTransactionRepository extends JpaRepository<AssetTransaction, UUID> {
    List<AssetTransaction> findByTransactionId(UUID transactionId);

    @Query("SELECT COALESCE(SUM(CASE WHEN at.movementType = 'ISSUED' THEN at.quantity WHEN at.movementType = 'RETURNED' THEN -at.quantity ELSE 0 END), 0) FROM AssetTransaction at WHERE at.transaction.id = :transactionId AND at.assetType.id = :assetTypeId")
    Integer calculateOutstandingQuantity(@Param("transactionId") UUID transactionId, @Param("assetTypeId") UUID assetTypeId);

    @Query("SELECT SUM(at.quantity) FROM AssetTransaction at WHERE at.transaction.id = :transactionId AND at.assetType.id = :assetTypeId AND at.movementType = :movementType")
    Integer sumQuantityByMovementType(@Param("transactionId") UUID transactionId, @Param("assetTypeId") UUID assetTypeId, @Param("movementType") String movementType);

    // ── Reporting aggregates ─────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(at) AS assetRecords,
                   COALESCE(SUM(CASE WHEN at.movementType = 'ISSUED' THEN at.quantity ELSE 0 END), 0) AS totalIssued,
                   COALESCE(SUM(CASE WHEN at.movementType = 'RETURNED' THEN at.quantity ELSE 0 END), 0) AS totalReturned,
                   COALESCE(SUM(CASE WHEN at.movementType = 'ISSUED' THEN at.quantity
                                     WHEN at.movementType = 'RETURNED' THEN -at.quantity
                                     ELSE 0 END), 0) AS totalOutstanding
            FROM AssetTransaction at
            WHERE at.transaction.lot.market.id = :marketId
              AND at.movementDate >= :from
              AND at.movementDate < :to
            """)
    AssetAggregateView summarizeForMarket(@Param("marketId") UUID marketId,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    /** Current outstanding crates across the whole market, ignoring the reporting period. */
    @Query("""
            SELECT COUNT(at) AS assetRecords,
                   COALESCE(SUM(CASE WHEN at.movementType = 'ISSUED' THEN at.quantity ELSE 0 END), 0) AS totalIssued,
                   COALESCE(SUM(CASE WHEN at.movementType = 'RETURNED' THEN at.quantity ELSE 0 END), 0) AS totalReturned,
                   COALESCE(SUM(CASE WHEN at.movementType = 'ISSUED' THEN at.quantity
                                     WHEN at.movementType = 'RETURNED' THEN -at.quantity
                                     ELSE 0 END), 0) AS totalOutstanding
            FROM AssetTransaction at
            WHERE at.transaction.lot.market.id = :marketId
            """)
    AssetAggregateView outstandingForMarket(@Param("marketId") UUID marketId);

    /**
     * Per transaction + asset type rollup used to render the asset ledger page without
     * loading the underlying movements.
     */
    @Query("""
            SELECT at.transaction.id,
                   at.assetType.id,
                   at.assetType.name,
                   COALESCE(SUM(CASE WHEN at.movementType = 'ISSUED' THEN at.quantity ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN at.movementType = 'RETURNED' THEN at.quantity ELSE 0 END), 0),
                   MIN(CASE WHEN at.movementType = 'ISSUED' THEN at.movementDate END),
                   MAX(CASE WHEN at.movementType = 'RETURNED' THEN at.movementDate END)
            FROM AssetTransaction at
            WHERE at.transaction.id IN :transactionIds
            GROUP BY at.transaction.id, at.assetType.id, at.assetType.name
            ORDER BY at.transaction.id, at.assetType.name
            """)
    List<Object[]> rollupByTransactionIds(@Param("transactionIds") List<UUID> transactionIds);
}

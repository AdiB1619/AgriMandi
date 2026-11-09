package com.agrimandi.api.repository;

import com.agrimandi.api.dto.report.ReportProjections.AuctionAggregateView;
import com.agrimandi.api.entity.Auction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    Optional<Auction> findByLotId(UUID lotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.lot.id = :lotId")
    Optional<Auction> findWithLockByLotId(@Param("lotId") UUID lotId);

    // ── Reporting aggregates ─────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(a) AS totalAuctions,
                   COALESCE(SUM(CASE WHEN a.status IN ('WINNER_CONFIRMED', 'CLOSED_NO_BIDS') THEN 1 ELSE 0 END), 0) AS completedAuctions,
                   COALESCE(SUM(CASE WHEN a.status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0) AS activeAuctions,
                   COALESCE(SUM(CASE WHEN a.tieDetected = TRUE THEN 1 ELSE 0 END), 0) AS tieCases,
                   COALESCE(SUM(a.lot.quantity), 0) AS totalQuantityAuctioned
            FROM Auction a
            WHERE a.lot.market.id = :marketId
              AND a.startedAt >= :from
              AND a.startedAt < :to
            """)
    AuctionAggregateView summarizeForPeriod(@Param("marketId") UUID marketId,
                                            @Param("from") OffsetDateTime from,
                                            @Param("to") OffsetDateTime to);

    @Query("""
            SELECT COUNT(a) AS total
            FROM Auction a
            WHERE a.lot.market.id = :marketId
              AND a.status = 'IN_PROGRESS'
            """)
    com.agrimandi.api.dto.report.ReportProjections.CountView countActiveForMarket(@Param("marketId") UUID marketId);

    @Query("""
            SELECT a FROM Auction a
            WHERE a.lot.market.id = :marketId
              AND a.status = 'IN_PROGRESS'
            ORDER BY a.startedAt DESC
            """)
    List<Auction> findActiveForMarket(@Param("marketId") UUID marketId, Pageable pageable);
}

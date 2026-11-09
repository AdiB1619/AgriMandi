package com.agrimandi.api.repository;

import com.agrimandi.api.dto.report.ReportProjections.CountView;
import com.agrimandi.api.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LotRepository extends JpaRepository<Lot, UUID> {
    Page<Lot> findByMarketId(UUID marketId, Pageable pageable);
    Page<Lot> findByStatus(String status, Pageable pageable);
    java.util.List<Lot> findByMarketId(UUID marketId);

    @Query("""
            SELECT COALESCE(SUM(l.quantity), 0)
            FROM Lot l
            WHERE l.market.id = :marketId
              AND l.createdAt >= :from
              AND l.createdAt < :to
            """)
    BigDecimal sumQuantityForPeriod(@Param("marketId") UUID marketId,
                                    @Param("from") OffsetDateTime from,
                                    @Param("to") OffsetDateTime to);

    @Query("""
            SELECT COUNT(l) AS total
            FROM Lot l
            WHERE l.market.id = :marketId
              AND l.createdAt >= :from
              AND l.createdAt < :to
            """)
    CountView countForPeriod(@Param("marketId") UUID marketId,
                             @Param("from") OffsetDateTime from,
                             @Param("to") OffsetDateTime to);

    @Query("""
            SELECT COUNT(l) AS total
            FROM Lot l
            WHERE l.market.id = :marketId
              AND l.status = :status
            """)
    CountView countByMarketAndStatus(@Param("marketId") UUID marketId, @Param("status") String status);
}

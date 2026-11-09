package com.agrimandi.api.repository;

import com.agrimandi.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, UUID entityId);

    /**
     * Market-scoped audit search. Rows whose market could not be resolved (legacy entries written
     * before the market column existed) are intentionally excluded from market reports.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.market.id = :marketId
              AND (:from IS NULL OR a.timestamp >= :from)
              AND (:to IS NULL OR a.timestamp < :to)
              AND (:actorId IS NULL OR a.actor.id = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLog> searchForMarket(@Param("marketId") UUID marketId,
                                    @Param("from") OffsetDateTime from,
                                    @Param("to") OffsetDateTime to,
                                    @Param("actorId") UUID actorId,
                                    @Param("action") String action,
                                    @Param("entityType") String entityType,
                                    @Param("entityId") UUID entityId,
                                    Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.market.id = :marketId
            ORDER BY a.timestamp DESC
            """)
    List<AuditLog> findRecentForMarket(@Param("marketId") UUID marketId, Pageable pageable);

    @Query("""
            SELECT DISTINCT a.action FROM AuditLog a
            WHERE a.market.id = :marketId
            ORDER BY a.action
            """)
    List<String> findDistinctActionsForMarket(@Param("marketId") UUID marketId);
}

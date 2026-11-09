package com.agrimandi.api.repository;

import com.agrimandi.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByTransactionId(UUID transactionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.transaction.id = :transactionId")
    BigDecimal sumAmountByTransactionId(@Param("transactionId") UUID transactionId);

    /**
     * Single grouped query for a whole page of ledger rows: paid total, payment count and
     * latest payment date per transaction. Avoids an N+1 lookup while rendering the ledger.
     */
    @Query("""
            SELECT p.transaction.id, COALESCE(SUM(p.amount), 0), COUNT(p), MAX(p.paymentDate)
            FROM Payment p
            WHERE p.transaction.id IN :transactionIds
            GROUP BY p.transaction.id
            """)
    List<Object[]> rollupByTransactionIds(@Param("transactionIds") List<UUID> transactionIds);
}

package com.council.tenderapp.prc.repositories;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.council.tenderapp.prc.entities.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long>
{
    @Query(value = """
        SELECT l.final_order_status
        FROM ledger_entries l 
        JOIN bill b 
        ON l.bill_id = b.id
        WHERE b.order_id = :orderId
        ORDER BY l.timestamp DESC
        LIMIT 1; 
    """, nativeQuery = true)
    String findFinalOrderStatusByOrderId(@Param("orderId") Long orderId);

    Optional<LedgerEntry> findByOrderId(@Param("orderId") Long orderId);

    Optional<String> findFinalOrderStatusById(@Param("ledgerId") Long ledgerId);

    Optional<OffsetDateTime> findTimestampById(@Param("ledgerId") Long ledgerId);

    Optional<Double> findTotalChargeById(@Param("ledgerId") Long ledgerId);
}

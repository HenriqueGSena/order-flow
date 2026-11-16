package com.backend.orderflow.repository;

import com.backend.orderflow.domain.Order;
import com.backend.orderflow.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO processed_events (
            event_id,
            processed_at
        )
        VALUES (
            :eventId,
            CURRENT_TIMESTAMP(6)
        )
        """, nativeQuery = true)
    int registerIfAbsent(@Param("eventId") String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT o
    FROM Order o
    WHERE o.id = :orderId
    """)
    Optional<Order> findByIdForUpdate(@Param("orderId") UUID orderId);
}

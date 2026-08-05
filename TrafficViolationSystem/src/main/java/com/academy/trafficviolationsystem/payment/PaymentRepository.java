package com.academy.trafficviolationsystem.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    // ── lookups ───────────────────────────────────────────────────────────

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    /** All payment attempts for a fine — newest first. */
    List<PaymentEntity> findByFineIdOrderByCreatedDesc(UUID fineId);

    /** Check if a successful payment already exists for a fine (idempotency guard). */
    boolean existsByFineIdAndStatus(UUID fineId, PaymentStatus status);

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    // ── reference number generation ───────────────────────────────────────

    /**
     * Counts payments created in a given year for transaction ID sequencing.
     * Format: TXN-{YYYYMMDD}-{count+1 padded to 6 digits}.
     */
    @Query("""
        SELECT COUNT(p) FROM PaymentEntity p
        WHERE p.created >= :yearStart AND p.created < :yearEnd
        """)
    long countByYear(@Param("yearStart") LocalDateTime yearStart,
                     @Param("yearEnd")   LocalDateTime yearEnd);

    // ── status and PDF updates ────────────────────────────────────────────

    @Modifying
    @Query("UPDATE PaymentEntity p SET p.status = :status WHERE p.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") PaymentStatus status);

    @Modifying
    @Query("UPDATE PaymentEntity p SET p.receiptPdfPath = :path WHERE p.id = :id")
    void setReceiptPdfPath(@Param("id") UUID id, @Param("path") String path);
}

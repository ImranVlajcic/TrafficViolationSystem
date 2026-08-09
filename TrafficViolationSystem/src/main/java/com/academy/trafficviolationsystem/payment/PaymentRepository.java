package com.academy.trafficviolationsystem.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    // ── status and PDF updates ────────────────────────────────────────────

    @Modifying
    @Query("UPDATE PaymentEntity p SET p.receiptPdfPath = :path WHERE p.id = :id")
    void setReceiptPdfPath(@Param("id") UUID id, @Param("path") String path);
}

package com.academy.trafficviolationsystem.fine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FineRepository extends JpaRepository<FineEntity, UUID> {

    // ── lookups ───────────────────────────────────────────────────────────

    Optional<FineEntity> findByFineNumber(String fineNumber);

    Optional<FineEntity> findByViolationId(UUID violationId);

    boolean existsByViolationId(UUID violationId);

    /** All fines for a driver, newest first — citizen portal and driver detail page. */
    List<FineEntity> findByDriverIdOrderByIssuedAtDesc(UUID driverId);

    // ── reference number generation ───────────────────────────────────────

    @Query("""
        SELECT COUNT(f) FROM FineEntity f
        WHERE f.issuedAt >= :yearStart AND f.issuedAt < :yearEnd
          AND f.deletedAt IS NULL
        """)
    long countByYear(@Param("yearStart") LocalDateTime yearStart,
                     @Param("yearEnd")   LocalDateTime yearEnd);

    // ── status updates ────────────────────────────────────────────────────

    // @Modifying bulk updates bypass the persistence context: Hibernate
    // never loads the row into memory, so JPA auditing (updated/updatedBy)
    // does not fire and any soft-deleted row would still be reachable
    // unless explicitly excluded. Both fixed below — same recurring pattern
    // flagged in Appeal/Fine reviews. f.updated is stamped manually since
    // the auditing listener isn't invoked for bulk JPQL updates.
    @Modifying
    @Query("""
        UPDATE FineEntity f
        SET f.status = :status, f.updated = CURRENT_TIMESTAMP
        WHERE f.id = :id AND f.deletedAt IS NULL
        """)
    void updateStatus(@Param("id") UUID id, @Param("status") FineStatus status);

    @Modifying
    @Query("""
        UPDATE FineEntity f
        SET f.pdfPath = :pdfPath, f.updated = CURRENT_TIMESTAMP
        WHERE f.id = :id AND f.deletedAt IS NULL
        """)
    void setPdfPath(@Param("id") UUID id, @Param("pdfPath") String pdfPath);

    // ── job queries ───────────────────────────────────────────────────────

    /**
     * All UNPAID fines whose due date has passed.
     * Used by OverdueFineCheckerJob to mark them OVERDUE and apply surcharge.
     */
    @Query("""
        SELECT f FROM FineEntity f
        WHERE f.status = 'UNPAID'
          AND f.dueDate < :today
          AND f.deletedAt IS NULL
        """)
    List<FineEntity> findUnpaidPassedDueDate(@Param("today") LocalDate today);

    /**
     * All UNPAID fines still within the early-pay window.
     * Used to compute discount eligibility in batch if needed.
     */
    @Query("""
        SELECT f FROM FineEntity f
        WHERE f.status = 'UNPAID'
          AND f.deletedAt IS NULL
          AND f.issuedAt >= :windowStart
        """)
    List<FineEntity> findUnpaidWithinEarlyPayWindow(@Param("windowStart") LocalDateTime windowStart);

    /**
     * Statistics query — total collected in a date range.
     * Used by AnalyticsService and ViolationAggregatorJob.
     */
    @Query("""
        SELECT COALESCE(SUM(f.totalDue), 0)
        FROM FineEntity f
        WHERE f.status = 'PAID'
          AND f.paidAt >= :from
          AND f.paidAt <  :to
          AND f.deletedAt IS NULL
        """)
    java.math.BigDecimal sumCollectedInRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    // Uses issuedAt (the domain-meaningful issuance timestamp), not the
    // inherited created field — was inconsistent with the rest of the
    // module, which keys every other date-range query off issuedAt.
    @Query("""
        SELECT COUNT(f) FROM FineEntity f
        WHERE f.issuedAt >= :start AND f.issuedAt < :end AND f.deletedAt IS NULL
        """)
    int countIssuedInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(f.totalDue), 0) FROM FineEntity f
        WHERE f.issuedAt >= :start AND f.issuedAt < :end AND f.deletedAt IS NULL
        """)
    BigDecimal sumAmountInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(f) FROM FineEntity f
        WHERE f.status = 'OVERDUE' AND f.issuedAt >= :start AND f.issuedAt < :end
          AND f.deletedAt IS NULL
        """)
    int countOverdueInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
    SELECT v.violationType, COUNT(f), COALESCE(SUM(f.totalDue), 0)
    FROM FineEntity f, ViolationEntity v
    WHERE f.violationId = v.id
      AND f.issuedAt >= :from AND f.issuedAt < :to
      AND f.deletedAt IS NULL
    GROUP BY v.violationType
    """)
    List<Object[]> aggregateByViolationTypeInRange(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}

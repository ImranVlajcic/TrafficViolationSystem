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
        """)
    long countByYear(@Param("yearStart") LocalDateTime yearStart,
                     @Param("yearEnd")   LocalDateTime yearEnd);

    // ── status updates ────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE FineEntity f SET f.status = :status WHERE f.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") FineStatus status);

    @Modifying
    @Query("""
        UPDATE FineEntity f
        SET f.pdfPath = :pdfPath
        WHERE f.id = :id
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
        """)
    java.math.BigDecimal sumCollectedInRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("SELECT COUNT(f) FROM FineEntity f WHERE f.created >= :start AND f.created < :end")
    int countIssuedInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(f.totalDue), 0) FROM FineEntity f WHERE f.created >= :start AND f.created < :end")
    BigDecimal sumAmountInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM FineEntity f " +
            "WHERE f.status = 'OVERDUE' AND f.created >= :start AND f.created < :end")
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

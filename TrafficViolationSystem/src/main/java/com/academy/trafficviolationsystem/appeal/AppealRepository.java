package com.academy.trafficviolationsystem.appeal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<ViolationAppealEntity, UUID> {

    // ── active appeal guard ───────────────────────────────────────────────

    /**
     * Finds any non-terminal appeal for a violation.
     * Used by AppealService.beforeInsert() to prevent duplicate active appeals.
     * Terminal statuses are APPROVED, REJECTED, WITHDRAWN.
     */
    @Query("""
        SELECT a FROM ViolationAppealEntity a
        WHERE a.violation.id = :violationId
          AND a.status NOT IN ('APPROVED', 'REJECTED', 'WITHDRAWN')
        """)
    Optional<ViolationAppealEntity> findActiveByViolationId(@Param("violationId") UUID violationId);

    // ── driver history ────────────────────────────────────────────────────

    /** All appeals by a driver — newest first. Used by citizen portal. */
    List<ViolationAppealEntity> findByDriverIdOrderBySubmittedAtDesc(UUID driverId);

    // ── officer review queues ─────────────────────────────────────────────

    /** Appeals in a given status ordered oldest-first (work through the queue in order). */
    List<ViolationAppealEntity> findByStatusOrderBySubmittedAtAsc(AppealStatus status);

    /** All appeals reviewed by a specific officer. */
    List<ViolationAppealEntity> findByReviewedByIdOrderByReviewedAtDesc(UUID reviewedById);

    // ── reference number generation ───────────────────────────────────────

    /**
     * Counts appeals submitted in a given year.
     * Used by AppealService.generateAppealNumber() for the sequence number.
     */
    @Query("""
        SELECT COUNT(a) FROM ViolationAppealEntity a
        WHERE a.submittedAt >= :yearStart AND a.submittedAt < :yearEnd
        """)
    long countByYear(@Param("yearStart") LocalDateTime yearStart,
                     @Param("yearEnd")   LocalDateTime yearEnd);

    // ── analytics ─────────────────────────────────────────────────────────

    /** Count of appeals in each status — used by ViolationAggregatorJob. */
    @Query("""
        SELECT a.status, COUNT(a)
        FROM ViolationAppealEntity a
        WHERE a.submittedAt >= :from AND a.submittedAt < :to
        GROUP BY a.status
        """)
    List<Object[]> countByStatusInRange(@Param("from") LocalDateTime from,
                                         @Param("to")   LocalDateTime to);

    @Query("SELECT COUNT(a) FROM ViolationAppealEntity a WHERE a.created >= :start AND a.created < :end")
    int countSubmittedInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM ViolationAppealEntity a " +
            "WHERE a.status = 'APPROVED' AND a.created >= :start AND a.created < :end")
    int countApprovedInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

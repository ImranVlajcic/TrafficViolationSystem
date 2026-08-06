package com.academy.trafficviolationsystem.violation;

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
public interface ViolationRepository extends JpaRepository<ViolationEntity, UUID> {

    // ── lookups ───────────────────────────────────────────────────────────

    Optional<ViolationEntity> findByReferenceNumber(String referenceNumber);

    boolean existsByReferenceNumber(String referenceNumber);

    /** All violations for a vehicle — used on the vehicle detail page. */
    List<ViolationEntity> findByVehicleIdOrderByOccurredAtDesc(UUID vehicleId);

    /** All violations for a driver — used on the driver detail page and citizen portal. */
    List<ViolationEntity> findByDriverIdOrderByOccurredAtDesc(UUID driverId);

    /** All pending violations for officer review queue. */
    List<ViolationEntity> findByStatusOrderByOccurredAtAsc(ViolationStatus status);

    // ── reference number generation ───────────────────────────────────────

    /**
     * Counts violations created in a given year to generate the next sequence number.
     * Used by ViolationService.generateReferenceNumber().
     */
    @Query("""
        SELECT COUNT(v) FROM ViolationEntity v
        WHERE v.created >= :yearStart AND v.created < :yearEnd
        """)
    long countByYear(@Param("yearStart") LocalDateTime yearStart,
                     @Param("yearEnd")   LocalDateTime yearEnd);

    // ── fine link ─────────────────────────────────────────────────────────

    /**
     * Called by FineService after creating a FineEntity to back-link
     * the violation to its fine. Raw UUID update — no JPA cascade needed.
     */
    @Modifying
    @Query("UPDATE ViolationEntity v SET v.fineId = :fineId WHERE v.id = :violationId")
    void setFineId(@Param("violationId") UUID violationId, @Param("fineId") UUID fineId);

    /**
     * Called by FineService when a fine is paid to close the violation.
     */
    @Modifying
    @Query("UPDATE ViolationEntity v SET v.status = :status WHERE v.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ViolationStatus status);

    // ── analytics / job queries ───────────────────────────────────────────

    /**
     * Finds all confirmed violations in a time window — used by
     * ViolationAggregatorJob to rebuild the heatmap data nightly.
     */
    @Query("""
        SELECT v FROM ViolationEntity v
        WHERE v.status IN ('CONFIRMED', 'CLOSED')
          AND v.occurredAt >= :from
          AND v.occurredAt <  :to
          AND v.deletedAt IS NULL
        """)
    List<ViolationEntity> findConfirmedInRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("SELECT COUNT(v) FROM ViolationEntity v " +
            "WHERE v.occurredAt >= :start " +
            "AND v.occurredAt < :end " +
            "AND v.isAutomatic = true " +
            "AND v.deletedAt IS NULL")
    int countAutoInRange(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);


    @Query("SELECT COUNT(v) FROM ViolationEntity v " +
            "WHERE v.occurredAt >= :start " +
            "AND v.occurredAt < :end " +
            "AND v.isAutomatic = false " +
            "AND v.deletedAt IS NULL")
    int countManualInRange(@Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    @Query("""
    SELECT v.officer.id, v.officer.username, COUNT(v)
    FROM ViolationEntity v
    WHERE v.occurredAt >= :from AND v.occurredAt < :to
      AND v.officer IS NOT NULL
      AND v.deletedAt IS NULL
    GROUP BY v.officer.id, v.officer.username
    ORDER BY COUNT(v) DESC
    """)
    List<Object[]> countByOfficerInRange(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}

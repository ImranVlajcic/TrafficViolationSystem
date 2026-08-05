package com.academy.trafficviolationsystem.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LicenseSuspensionRepository extends JpaRepository<LicenseSuspensionEntity, UUID> {

    /** Returns the current active suspension for a driver (at most one at a time). */
    Optional<LicenseSuspensionEntity> findByDriverIdAndIsActiveTrue(UUID driverId);

    /** Full history for a driver, newest first. */
    List<LicenseSuspensionEntity> findByDriverIdOrderByStartDateDesc(UUID driverId);

    /** Used by the nightly job to find suspensions that should be lifted today. */
    @Query("""
        SELECT s FROM LicenseSuspensionEntity s
        WHERE s.isActive = true
          AND s.endDate IS NOT NULL
          AND s.endDate <= :today
        """)
    List<LicenseSuspensionEntity> findExpiredActiveSuspensions(@Param("today") LocalDate today);

    /** Deactivate all active suspensions for a driver (used on manual lift). */
    @Modifying
    @Query("""
        UPDATE LicenseSuspensionEntity s
        SET s.isActive = false, s.liftedAt = :today
        WHERE s.driver.id = :driverId AND s.isActive = true
        """)
    void liftAllForDriver(@Param("driverId") UUID driverId, @Param("today") LocalDate today);
}

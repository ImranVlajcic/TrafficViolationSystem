package com.academy.trafficviolationsystem.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleOwnershipHistoryRepository extends JpaRepository<VehicleOwnershipHistoryEntity, UUID> {

    /** Full ownership history for a vehicle, newest transfer first. */
    List<VehicleOwnershipHistoryEntity> findByVehicleIdOrderByTransferDateDesc(UUID vehicleId);

    /**
     * Finds who owned a vehicle on a specific date.
     * Used by ViolationService to resolve owner at the time of an old violation.
     *
     * Logic: find the most recent transfer whose transferDate <= targetDate.
     * That transfer's newOwner was the owner on that date.
     */
    @Query("""
        SELECT h FROM VehicleOwnershipHistoryEntity h
        WHERE h.vehicle.id = :vehicleId
          AND h.transferDate <= :targetDate
        ORDER BY h.transferDate DESC
        LIMIT 1
        """)
    Optional<VehicleOwnershipHistoryEntity> findOwnerAtDate(
            @Param("vehicleId")   UUID vehicleId,
            @Param("targetDate")  LocalDate targetDate);
}

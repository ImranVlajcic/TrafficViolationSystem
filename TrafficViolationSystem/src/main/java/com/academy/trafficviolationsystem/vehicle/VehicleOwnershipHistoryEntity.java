package com.academy.trafficviolationsystem.vehicle;

import com.academy.trafficviolationsystem.core.entities.UUIDBaseEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * Append-only record of every vehicle ownership transfer.
 *
 * Critical for the violation module: when a violation is disputed and the
 * driver claims they had already sold the vehicle, this table provides the
 * evidence — it shows exactly who owned the vehicle on any given date.
 *
 * VehicleService.transferOwnership() writes a row here before updating
 * VehicleEntity.owner. Rows are never updated or soft-deleted.
 *
 * previousOwner is null only for the very first registration (no prior owner).
 */
@Getter
@Setter
@Entity
@SQLRestriction("deleted IS NULL")
@SQLDelete(sql = "UPDATE vehicle_ownership_history SET deleted = now() WHERE id = ?")
@Table(
    name = "vehicle_ownership_history",
    indexes = {
        @Index(name = "idx_voh_vehicle",      columnList = "vehicle_id, transfer_date DESC"),
        @Index(name = "idx_voh_new_owner",    columnList = "new_owner_id"),
        @Index(name = "idx_voh_prev_owner",   columnList = "previous_owner_id")
    }
)
public class VehicleOwnershipHistoryEntity extends UUIDBaseEntity {

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── relationships ─────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "vehicle_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_voh_vehicle")
    )
    private VehicleEntity vehicle;

    /** Null only for the first registration of a brand-new vehicle. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "previous_owner_id",
        foreignKey = @ForeignKey(name = "fk_voh_prev_owner")
    )
    private DriverEntity previousOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "new_owner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_voh_new_owner")
    )
    private DriverEntity newOwner;
}

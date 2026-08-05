package com.academy.trafficviolationsystem.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * Adds audit timestamps and actor tracking to AbstractEntity.
 *
 * All four fields are populated automatically by Spring Data JPA auditing,
 * which is enabled in JpaAuditingConfig. You never set them manually.
 *
 *   created    — set once when the entity is first persisted, never changed
 *   updated    — updated on every save()
 *   createdBy  — username of the user who created the record (from AuditorAware)
 *   updatedBy  — username of the last user who modified the record
 *
 * For background jobs and MQTT events where no user is authenticated,
 * AuditorAware returns "SYSTEM" so these fields are never null.
 *
 * Every domain entity in this project extends either:
 *   UUIDBaseEntity    (extends BaseEntity) — for domain tables (violations, fines…)
 *   AutoIdBaseEntity  (extends BaseEntity) — for config/reference tables
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity extends AbstractEntity {

    @CreatedDate
    @Column(name = "created", updatable = false)
    private Instant created;

    @LastModifiedDate
    @Column(name = "updated")
    private Instant updated;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}

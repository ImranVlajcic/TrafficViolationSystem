package com.academy.trafficviolationsystem.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Base entity for all domain objects that require a UUID primary key.
 * Prefer this over AutoIdBaseEntity for security-sensitive entities
 * (violations, fines, payments, drivers) where sequential integer IDs
 * would allow enumeration attacks.
 *
 * Extends BaseEntity, so it also inherits:
 *   - deletedAt  (soft-delete, from AbstractEntity)
 *   - created    (auto-set on persist, from BaseEntity)
 *   - updated    (auto-set on merge,   from BaseEntity)
 */
@Getter
@Setter
@MappedSuperclass
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class UUIDBaseEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;
}

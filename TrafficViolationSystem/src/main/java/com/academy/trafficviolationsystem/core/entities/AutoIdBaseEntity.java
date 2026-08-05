package com.academy.trafficviolationsystem.core.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Base entity for all domain objects that are not security-sensitive entities
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
public abstract class AutoIdBaseEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private Long id;
}
package com.academy.trafficviolationsystem.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Root of the entity hierarchy.
 *
 * Provides soft-delete support via deletedAt. When JPA's delete() is called
 * on any entity, @PreRemove fires and sets this timestamp instead of issuing
 * a DELETE statement. The row stays in the database but BaseService.search()
 * filters it out unless includeDeleted = true.
 *
 * @EntityListeners(AuditingEntityListener.class) wires up Spring Data's
 * @CreatedDate and @LastModifiedDate on the subclass BaseEntity.
 */
@Getter
@Setter
@MappedSuperclass
@SQLRestriction("deleted IS NULL")
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity {

    @Column(name = "deleted")
    private Instant deletedAt;

    @PreRemove
    public void preRemove() {
        this.deletedAt = Instant.now();
    }
}

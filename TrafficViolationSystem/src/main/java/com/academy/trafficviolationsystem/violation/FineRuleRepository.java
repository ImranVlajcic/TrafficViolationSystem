package com.academy.trafficviolationsystem.violation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FineRuleRepository extends JpaRepository<FineRuleEntity, Integer> {

    /**
     * Primary lookup used by FineService when issuing a fine.
     * Returns the active rule for the violation's type.
     */
    Optional<FineRuleEntity> findByViolationTypeAndIsActiveTrue(ViolationType violationType);

    boolean existsByViolationTypeAndIsActiveTrue(ViolationType violationType);

    boolean existsByViolationType(ViolationType violationType);
}

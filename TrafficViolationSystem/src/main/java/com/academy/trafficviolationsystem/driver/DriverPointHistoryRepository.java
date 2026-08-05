package com.academy.trafficviolationsystem.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DriverPointHistoryRepository extends JpaRepository<DriverPointHistoryEntity, UUID> {

    /** Full point history for a driver, newest event first. */
    List<DriverPointHistoryEntity> findByDriverIdOrderByOccurredAtDesc(UUID driverId);
}

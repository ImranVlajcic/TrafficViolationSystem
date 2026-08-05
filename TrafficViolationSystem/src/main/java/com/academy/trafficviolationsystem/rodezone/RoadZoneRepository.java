package com.academy.trafficviolationsystem.rodezone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for RoadZoneEntity.
 *
 * Extends JpaRepository only — JpaSpecificationExecutor removed because
 * RoadZoneService uses the EntityManager Criteria API (matching the core
 * BaseService.search() pattern) rather than Spring Data Specifications.
 */
@Repository
public interface RoadZoneRepository extends JpaRepository<RoadZoneEntity, Integer> {

    /** Active zones ordered alphabetically — for map layer and camera-assignment dropdowns. */
    List<RoadZoneEntity> findByIsActiveTrueOrderByNameAsc();

    /** All zones of a specific type (active and inactive). */
    List<RoadZoneEntity> findByZoneType(ZoneType zoneType);
}

package com.academy.trafficviolationsystem.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ViolationLocationLogRepository extends JpaRepository<ViolationLocationLogEntity, UUID> {

    /**
     * Cluster raw log entries into approximate grid cells (0.001° ≈ 111 m).
     * Used by ViolationAggregatorJob to build AccidentHotspotEntity records.
     */
    @Query("""
        SELECT ROUND(v.latitude  / :gridSize) * :gridSize AS lat,
               ROUND(v.longitude / :gridSize) * :gridSize AS lon,
               COUNT(v)                                    AS cnt,
               v.violationType                             AS vtype
        FROM ViolationLocationLogEntity v
        WHERE v.occurredAt BETWEEN :from AND :to
        GROUP BY ROUND(v.latitude  / :gridSize),
                 ROUND(v.longitude / :gridSize),
                 v.violationType
        ORDER BY cnt DESC
        """)
    List<Object[]> clusterByGridCell(
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to,
            @Param("gridSize")  double gridSize);

    void deleteByOccurredAtBefore(LocalDateTime cutoff);
}

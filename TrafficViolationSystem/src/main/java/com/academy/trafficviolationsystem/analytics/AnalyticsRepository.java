package com.academy.trafficviolationsystem.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<AccidentHotspotEntity, Integer> {

    List<AccidentHotspotEntity> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);

    void deleteByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);

    @Query("""
        SELECT h FROM AccidentHotspotEntity h
        WHERE h.periodEnd = :periodEnd
        ORDER BY h.severityScore DESC
        LIMIT 10
        """)
    List<AccidentHotspotEntity> findTop10BySeverityScoreDescAndPeriodEnd(
            @Param("periodEnd") LocalDate periodEnd);
}

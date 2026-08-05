package com.academy.trafficviolationsystem.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SystemStatisticsRepository extends JpaRepository<SystemStatisticsEntity, Integer> {

    Optional<SystemStatisticsEntity> findByPeriodTypeAndPeriodStartAndPeriodEnd(
            PeriodType periodType, LocalDate periodStart, LocalDate periodEnd);

    void deleteByPeriodTypeAndPeriodStartAndPeriodEnd(
            PeriodType periodType, LocalDate periodStart, LocalDate periodEnd);
}

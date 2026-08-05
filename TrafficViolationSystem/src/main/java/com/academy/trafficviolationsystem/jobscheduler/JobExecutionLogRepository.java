package com.academy.trafficviolationsystem.jobscheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLogEntity, Integer> {

    List<JobExecutionLogEntity> findByJobNameOrderByStartedAtDesc(String jobName);

    List<JobExecutionLogEntity> findByStatus(JobStatus status);

    /**
     * Finds RUNNING rows that started more than {@code staleThreshold} ago —
     * indicates a crashed job that never wrote its final status.
     */
    @Query("""
        SELECT j FROM JobExecutionLogEntity j
        WHERE j.status = 'RUNNING'
          AND j.startedAt < :staleThreshold
        """)
    List<JobExecutionLogEntity> findStuckJobs(@Param("staleThreshold") LocalDateTime staleThreshold);
}

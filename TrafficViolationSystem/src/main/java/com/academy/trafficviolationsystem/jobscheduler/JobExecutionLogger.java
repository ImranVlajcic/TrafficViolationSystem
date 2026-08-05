package com.academy.trafficviolationsystem.jobscheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Thin helper so every job can open/close a log row without duplicating
 * save/update boilerplate. Uses REQUIRES_NEW so the log survives a
 * rolled-back job transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionLogger {

    private final JobExecutionLogRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobExecutionLogEntity start(String jobName, String triggeredBy) {
        JobExecutionLogEntity entry = JobExecutionLogEntity.builder()
                .jobName(jobName)
                .startedAt(LocalDateTime.now())
                .status(JobStatus.RUNNING)
                .recordsProcessed(0)
                .triggeredBy(triggeredBy)
                .build();
        return repo.save(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(JobExecutionLogEntity entry, int recordsProcessed) {
        entry.setStatus(JobStatus.SUCCESS);
        entry.setRecordsProcessed(recordsProcessed);
        entry.setFinishedAt(LocalDateTime.now());
        repo.save(entry);
        log.info("[{}] finished — {} records processed", entry.getJobName(), recordsProcessed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(JobExecutionLogEntity entry, Exception ex) {
        entry.setStatus(JobStatus.FAILED);
        entry.setErrorMessage(ex.getMessage());
        entry.setFinishedAt(LocalDateTime.now());
        repo.save(entry);
        log.error("[{}] FAILED: {}", entry.getJobName(), ex.getMessage(), ex);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void skipped(JobExecutionLogEntity entry, String reason) {
        entry.setStatus(JobStatus.SKIPPED);
        entry.setErrorMessage(reason);
        entry.setFinishedAt(LocalDateTime.now());
        repo.save(entry);
        log.info("[{}] skipped — {}", entry.getJobName(), reason);
    }
}

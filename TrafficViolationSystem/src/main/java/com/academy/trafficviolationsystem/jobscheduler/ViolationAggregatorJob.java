package com.academy.trafficviolationsystem.jobscheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Daily 02:00 — rebuilds AccidentHotspotEntity records for yesterday,
 * writes DAILY SystemStatisticsEntity, and on the right calendar days
 * also writes WEEKLY / MONTHLY snapshots.
 *
 * Heavy lifting is in {@link AggregationService}; this class only owns
 * scheduling and JobExecutionLog bookkeeping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViolationAggregatorJob implements JobCommand{

    private static final String JOB_NAME = "ViolationAggregatorJob";

    private final AggregationService aggregationService;
    private final JobExecutionLogger jobLogger;

    @Override
    public String getJobName() {
        return JOB_NAME;
    }


    @Override
    public void execute() {
        log.info("[{}] starting", JOB_NAME);
        JobExecutionLogEntity logEntry = jobLogger.start(JOB_NAME, "SCHEDULER");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            int count = aggregationService.runDailyAggregation(yesterday);
            jobLogger.success(logEntry, count);
        } catch (Exception ex) {
            jobLogger.failed(logEntry, ex);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runScheduled() {
        execute();
    }
}

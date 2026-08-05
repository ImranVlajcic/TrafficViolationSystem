package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.fine.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily 01:00 — finds all UNPAID fines whose dueDate < today,
 * applies the late-payment surcharge, and transitions them to OVERDUE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueFineCheckerJob implements JobCommand {

    private static final String JOB_NAME = "OverdueFineCheckerJob";

    private final FineService        fineService;
    private final JobExecutionLogger jobLogger;

    @Override
    public String getJobName() {
        return JOB_NAME;
    }

    @Override
    public void execute() {
        log.info("[{}] starting", JOB_NAME);
        // Variable renamed from 'log' to 'logEntry' to avoid shadowing the Slf4j logger field
        JobExecutionLogEntity logEntry = jobLogger.start(JOB_NAME, "SCHEDULER");
        try {
            int count = fineService.markOverdueWithSurcharge();
            jobLogger.success(logEntry, count);
        } catch (Exception ex) {
            jobLogger.failed(logEntry, ex);
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void runScheduled() {
        execute();
    }

}

package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every 15 minutes — re-dispatches RETRYING notifications whose
 * nextRetryAt <= now().
 *
 * Exponential backoff managed by NotificationService:
 *   attempt 1 → retry in  5 min
 *   attempt 2 → retry in 15 min
 *   attempt 3 → retry in 60 min
 *   > 3       → mark FAILED permanently
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob implements JobCommand{

    private static final String JOB_NAME = "NotificationRetryJob";

    private final NotificationService notificationService;
    private final JobExecutionLogger  jobLogger;

    @Override
    public String getJobName() {
        return JOB_NAME;
    }

    @Override
    public void execute() {
        log.debug("[{}] checking for retryable notifications", JOB_NAME);
        JobExecutionLogEntity logEntry = jobLogger.start(JOB_NAME, "SCHEDULER");
        try {
            int count = notificationService.retryFailed();
            jobLogger.success(logEntry, count);
        } catch (Exception ex) {
            jobLogger.failed(logEntry, ex);
        }
    }

    @Scheduled(fixedDelay = 900_000) //15 minutes
    public void runScheduled() {
        execute();
    }
}

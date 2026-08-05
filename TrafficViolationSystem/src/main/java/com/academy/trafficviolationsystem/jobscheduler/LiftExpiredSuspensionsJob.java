package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.driver.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lifts driver suspensions whose suspension period has expired.
 *
 * Runs every day at 03:15 and can also be
 * triggered manually through the JobCommand interface.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiftExpiredSuspensionsJob implements JobCommand {

    private static final String JOB_NAME = "LiftExpiredSuspensionsJob";

    private final DriverService driverService;
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
            driverService.liftExpiredSuspensions();

            // DriverService currently returns void.
            jobLogger.success(logEntry, 1);
        } catch (Exception ex) {
            jobLogger.failed(logEntry, ex);
        }
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void runScheduled() {
        execute();
    }
}
package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.driver.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Resets all driver penalty points once per year.
 *
 * Runs automatically on January 1st at 03:00 and can also be
 * triggered manually through the JobCommand interface.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LicensePointResetJob implements JobCommand {

    private static final String JOB_NAME = "LicensePointResetJob";

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
            driverService.resetAllPenaltyPoints();

            // DriverService currently returns void.
            jobLogger.success(logEntry, 1);
        } catch (Exception ex) {
            jobLogger.failed(logEntry, ex);
        }
    }

    @Scheduled(cron = "0 0 3 1 1 *")
    public void runScheduled() {
        execute();
    }
}
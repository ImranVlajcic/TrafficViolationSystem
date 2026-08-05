package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.camera.CameraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every 5 minutes — two tasks in one pass:
 *  1. markStaleAsOffline()  — cameras with lastHeartbeatAt older than 10 min → OFFLINE.
 *  2. retryFailedEvents()   — reprocesses unprocessed CameraEventEntity rows (retryCount < 3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CameraHeartbeatJob implements JobCommand {

    private static final String JOB_NAME = "CameraHeartbeatJob";

    private final CameraService      cameraService;
    private final JobExecutionLogger jobLogger;

    @Override
    public String getJobName() {
        return JOB_NAME;
    }

    @Override
    public void execute() {
        log.debug("[{}] starting heartbeat check", JOB_NAME);
        JobExecutionLogEntity logEntry = jobLogger.start(JOB_NAME, "SCHEDULER");
        try {
            int offlineMarked  = cameraService.markStaleAsOffline();
            int eventsRetried  = cameraService.retryFailedEvents();
            jobLogger.success(logEntry, offlineMarked + eventsRetried);
        } catch (Exception ex) {
            jobLogger.failed(logEntry, ex);
        }
    }

    @Scheduled(fixedDelay = 300_000) // 5 minutes
    public void runScheduled() {
        execute();
    }
}

package com.academy.trafficviolationsystem.jobscheduler;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin-only endpoints to manually trigger jobs and inspect execution history.
 * All routes are under /api/admin/jobs.
 *
 * Returns JobExecutionLogDto (not the raw entity) to avoid LazyInitializationException
 * and to keep the API contract independent of the JPA model.
 */
@RestController
@RequestMapping("/api/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
public class JobAdminController {

    private final JobExecutionLogRepository logRepository;
    private final Map<String, JobCommand> jobs;

    public JobAdminController(List<JobCommand> commands,
                              JobExecutionLogRepository logRepository) {

        this.jobs = commands.stream()
                .collect(Collectors.toMap(
                        JobCommand::getJobName,
                        Function.identity()));

        this.logRepository = logRepository;
    }

    // ── Manual triggers ───────────────────────────────────────────────────────

    @PostMapping("/overdue-fines/trigger")
    public ResponseEntity<String> triggerOverdueFines() {
        jobs.get("OverdueFineCheckerJob").execute();
        return ResponseEntity.ok("OverdueFineCheckerJob triggered");
    }

    @PostMapping("/point-reset/trigger")
    public ResponseEntity<String> triggerPointReset() {
        jobs.get("LicensePointResetJob").execute();
        return ResponseEntity.ok("LicensePointResetJob triggered");
    }

    @PostMapping("/lift-expired-suspensions/trigger")
    public ResponseEntity<String> triggerLiftExpiredSuspensions() {
        jobs.get("LiftExpiredSuspensionsJob").execute();
        return ResponseEntity.ok("LiftExpiredSuspensionsJob triggered");
    }
    @PostMapping("/aggregator/trigger")
    public ResponseEntity<String> triggerAggregator() {
        jobs.get("ViolationAggregatorJob").execute();
        return ResponseEntity.ok("ViolationAggregatorJob triggered");
    }

    @PostMapping("/notification-retry/trigger")
    public ResponseEntity<String> triggerNotificationRetry() {
        jobs.get("NotificationRetryJob").execute();
        return ResponseEntity.ok("NotificationRetryJob triggered");
    }

    @PostMapping("/camera-heartbeat/trigger")
    public ResponseEntity<String> triggerCameraHeartbeat() {
        jobs.get("CameraHeartbeatJob").execute();
        return ResponseEntity.ok("CameraHeartbeatJob triggered");
    }

    // ── Log inspection ────────────────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<List<JobExecutionLogDto>> getLogs(
            @RequestParam(required = false) String jobName) {
        List<JobExecutionLogEntity> entities = jobName != null
                ? logRepository.findByJobNameOrderByStartedAtDesc(jobName)
                : logRepository.findAll();
        return ResponseEntity.ok(toDto(entities));
    }

    @GetMapping("/logs/stuck")
    public ResponseEntity<List<JobExecutionLogDto>> getStuckJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        return ResponseEntity.ok(toDto(logRepository.findStuckJobs(threshold)));
    }

    @GetMapping("/logs/failed")
    public ResponseEntity<List<JobExecutionLogDto>> getFailedJobs() {
        return ResponseEntity.ok(toDto(logRepository.findByStatus(JobStatus.FAILED)));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private List<JobExecutionLogDto> toDto(List<JobExecutionLogEntity> entities) {
        return entities.stream()
                .map(JobExecutionLogDto::from)
                .collect(Collectors.toList());
    }
}

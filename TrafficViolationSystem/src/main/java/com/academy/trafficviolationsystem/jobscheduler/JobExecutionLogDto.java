package com.academy.trafficviolationsystem.jobscheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Read-only projection of JobExecutionLogEntity for API responses.
 *
 * Returning the raw JPA entity from REST endpoints risks LazyInitializationException
 * and exposes internal fields. This DTO is a safe, flat projection of what
 * the admin dashboard needs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionLogDto {

    private Integer id;
    private String jobName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private JobStatus status;
    private int recordsProcessed;
    private String errorMessage;
    private String triggeredBy;

    /** Computed: duration in seconds. Null if job has not finished yet. */
    private Long durationSeconds;

    /** Factory method — maps entity to DTO and computes durationSeconds. */
    public static JobExecutionLogDto from(JobExecutionLogEntity entity) {
        Long duration = null;
        if (entity.getStartedAt() != null && entity.getFinishedAt() != null) {
            duration = java.time.Duration.between(
                entity.getStartedAt(), entity.getFinishedAt()).getSeconds();
        }
        return JobExecutionLogDto.builder()
                .id(Math.toIntExact(entity.getId()))
                .jobName(entity.getJobName())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .status(entity.getStatus())
                .recordsProcessed(entity.getRecordsProcessed())
                .errorMessage(entity.getErrorMessage())
                .triggeredBy(entity.getTriggeredBy())
                .durationSeconds(duration)
                .build();
    }
}

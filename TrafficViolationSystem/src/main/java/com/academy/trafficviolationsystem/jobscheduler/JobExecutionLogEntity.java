package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.core.entities.AutoIdBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "job_execution_log",
    indexes = {
        @Index(name = "idx_job_name", columnList = "job_name, started_at DESC")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobExecutionLogEntity extends AutoIdBaseEntity {

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = true)
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private int recordsProcessed = 0;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String errorMessage;

    @Column(nullable = false)
    private String triggeredBy = "SCHEDULER";
}

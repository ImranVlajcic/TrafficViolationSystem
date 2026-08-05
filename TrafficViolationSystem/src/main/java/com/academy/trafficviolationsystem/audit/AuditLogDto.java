package com.academy.trafficviolationsystem.audit;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of AuditLogEntity.
 * All fields map directly — no computed fields needed.
 */
@Getter
@Setter
public class AuditLogDto {

    private UUID id;
    private String action;
    private String entityType;
    private UUID entityId;
    private UUID actorId;
    private String actorUsername;
    private String ipAddress;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String description;
    private LocalDateTime occurredAt;
    private Instant created;
}

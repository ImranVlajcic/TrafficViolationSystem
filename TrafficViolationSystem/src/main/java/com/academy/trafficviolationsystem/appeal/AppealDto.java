package com.academy.trafficviolationsystem.appeal;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of ViolationAppealEntity.
 *
 * violationReference is populated by AppealService after a ViolationRepository
 * lookup — not mapped by MapStruct directly.
 *
 * daysOpen is a computed field set by AppealMapper @AfterMapping showing
 * how many days the appeal has been open, so the officer review queue can
 * highlight stale appeals that need attention.
 */
@Getter
@Setter
public class AppealDto {

    private UUID id;
    private String appealNumber;

    // appeal content
    private String reason;
    private String evidenceUrl;

    // status & timeline
    private AppealStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewNotes;

    /**
     * Computed by AppealMapper @AfterMapping.
     * Days elapsed since submittedAt. Helps officers prioritise older appeals.
     */
    private long daysOpen;

    // violation summary
    private UUID violationId;

    /**
     * Populated by AppealService.toDtoWithDetails() from ViolationRepository.
     * Not set by MapStruct — requires a separate lookup.
     */
    private String violationReference;

    // fine link
    private UUID fineId;

    // driver summary
    private UUID driverId;
    private String driverFullName;
    private String driverLicenseNumber;

    // reviewer summary
    private UUID reviewedById;
    private String reviewedByFullName;

    // audit
    private Instant created;
}

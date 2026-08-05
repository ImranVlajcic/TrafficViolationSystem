package com.academy.trafficviolationsystem.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activates Spring's {@code @Scheduled} support.
 *
 * Without this class, all {@code @Scheduled} annotations in the application will
 * be silently ignored at startup — the beans load but no tasks fire.
 *
 * Cron expressions used in this project (defined in the job classes):
 *   OverdueFineCheckerJob     — every day at 01:00
 *   LicensePointResetJob      — 1st of every month at 03:00
 *   ViolationAggregatorJob    — every day at 02:00 (builds heatmap data)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // No beans needed — @EnableScheduling is the only purpose of this class.
}

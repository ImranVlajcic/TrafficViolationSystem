package com.academy.trafficviolationsystem.jobscheduler;

import com.academy.trafficviolationsystem.analytics.*;
import com.academy.trafficviolationsystem.appeal.AppealRepository;
import com.academy.trafficviolationsystem.camera.CameraRepository;
import com.academy.trafficviolationsystem.fine.FineRepository;
import com.academy.trafficviolationsystem.violation.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all heavy aggregation logic used by ViolationAggregatorJob.
 * Extracted here so it can also be triggered manually via AdminController
 * without duplicating scheduling annotations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private static final double GRID_SIZE = 0.001; // ~111 m per cell

    private final AnalyticsRepository            hotspotRepo;
    private final ViolationLocationLogRepository locationLogRepo;
    private final SystemStatisticsRepository     statisticsRepo;

    private final ViolationRepository violationRepo;
    private final FineRepository fineRepo;
    private final AppealRepository appealRepo;
    private final CameraRepository cameraRepo;

    /**
     * Full daily run for the given date.
     * Also triggers weekly / monthly snapshots when the calendar day demands it.
     *
     * @return total hotspot records written (used for JobExecutionLog.recordsProcessed)
     */
    @Transactional
    public int runDailyAggregation(LocalDate date) {
        int count = rebuildHotspots(date, date);
        writeStatisticsSnapshot(PeriodType.DAILY, date, date);

        // Weekly snapshot every Sunday
        if (date.getDayOfWeek().getValue() == 7) {
            LocalDate weekStart = date.minusDays(6);
            rebuildHotspots(weekStart, date);
            writeStatisticsSnapshot(PeriodType.WEEKLY, weekStart, date);
        }

        // Monthly snapshot on last day of month
        if (date.getDayOfMonth() == date.lengthOfMonth()) {
            LocalDate monthStart = date.withDayOfMonth(1);
            rebuildHotspots(monthStart, date);
            writeStatisticsSnapshot(PeriodType.MONTHLY, monthStart, date);
        }

        return count;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private int rebuildHotspots(LocalDate from, LocalDate to) {
        hotspotRepo.deleteByPeriodStartAndPeriodEnd(from, to);

        List<Object[]> clusters = locationLogRepo.clusterByGridCell(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), GRID_SIZE);

        List<AccidentHotspotEntity> hotspots = new ArrayList<>();
        for (Object[] row : clusters) {
            double lat   = (Double) row[0];
            double lon   = (Double) row[1];
            int    count = ((Number) row[2]).intValue();
            String type  = (String) row[3];

            hotspots.add(AccidentHotspotEntity.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .radiusMeters(100)
                    .violationCount(count)
                    .dominantType(type)
                    .periodStart(from)
                    .periodEnd(to)
                    .severityScore(count * severityWeight(type))
                    .build());
        }

        hotspotRepo.saveAll(hotspots);
        log.info("Rebuilt {} hotspot records for {} – {}", hotspots.size(), from, to);
        return hotspots.size();
    }

    private void writeStatisticsSnapshot(PeriodType periodType, LocalDate from, LocalDate to) {
        statisticsRepo.deleteByPeriodTypeAndPeriodStartAndPeriodEnd(periodType, from, to);

        List<Object[]> clusters = locationLogRepo.clusterByGridCell(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), 1.0);
        int totalViolations = clusters.stream()
                .mapToInt(row -> ((Number) row[2]).intValue())
                .sum();

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd   = to.plusDays(1).atStartOfDay();

        SystemStatisticsEntity snapshot = SystemStatisticsEntity.builder()
                .periodType(periodType)
                .periodStart(from)
                .periodEnd(to)
                .totalViolations(totalViolations)
                .autoDetected(violationRepo.countAutoInRange(rangeStart, rangeEnd))
                .manuallyRecorded(violationRepo.countManualInRange(rangeStart, rangeEnd))
                .totalFinesIssued(fineRepo.countIssuedInRange(rangeStart, rangeEnd))
                .totalFinesAmount(fineRepo.sumAmountInRange(rangeStart, rangeEnd))
                .totalCollected(fineRepo.sumCollectedInRange(rangeStart, rangeEnd))
                .totalOverdue(fineRepo.countOverdueInRange(rangeStart, rangeEnd))
                .appealsSubmitted(appealRepo.countSubmittedInRange(rangeStart, rangeEnd))
                .appealsApproved(appealRepo.countApprovedInRange(rangeStart, rangeEnd))
                .activeCameras(cameraRepo.countCurrentlyOnline())
                .computedAt(LocalDateTime.now())
                .build();

        statisticsRepo.save(snapshot);
        log.info("Saved {} statistics snapshot for {} – {} (violations={})",
                periodType, from, to, totalViolations);
    }

    private double severityWeight(String violationType) {
        if (violationType == null) return 1.0;
        return switch (violationType.toUpperCase()) {
            case "RUNNING_RED_LIGHT" -> 3.0;
            case "SPEEDING_SEVERE"   -> 2.5;
            case "DUI"               -> 4.0;
            case "WRONG_WAY"         -> 3.5;
            case "SPEEDING"          -> 1.5;
            default                  -> 1.0;
        };
    }
}

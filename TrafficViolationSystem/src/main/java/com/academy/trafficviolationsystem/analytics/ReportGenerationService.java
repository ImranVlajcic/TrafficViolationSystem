package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.appeal.AppealDto;
import com.academy.trafficviolationsystem.appeal.AppealService;
import com.academy.trafficviolationsystem.core.exceptions.BadRequestException;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.driver.*;
import com.academy.trafficviolationsystem.fine.FineDto;
import com.academy.trafficviolationsystem.fine.FineRepository;
import com.academy.trafficviolationsystem.fine.FineService;
import com.academy.trafficviolationsystem.violation.ViolationDto;
import com.academy.trafficviolationsystem.violation.ViolationRepository;
import com.academy.trafficviolationsystem.violation.ViolationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Each public method is fire-and-forget (@Async).
 * The dispatcher {@link #generateAsync(GeneratedReportEntity)} routes to the correct builder.
 * Each builder: GENERATING → build file → DONE  (or FAILED on exception).
 * Actual file rendering is delegated to a {@link ReportExportStrategy} chosen by {@link ReportFormat}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    @Value("${app.pdf.output-dir}")
    private String outputDir;

    private final GeneratedReportRepository reportRepository;
    private final FineRepository fineRepository;
    private final ViolationRepository violationRepository;
    private final AnalyticsRepository analyticsRepository;
    private final DriverRepository driverRepository;
    private final DriverService driverService;
    private final ViolationService violationService;
    private final FineService fineService;
    private final AppealService appealService;
    private final ObjectMapper objectMapper;
    private final List<ReportExportStrategy> exportStrategies;

    private Map<ReportFormat, ReportExportStrategy> strategiesByFormat;

    @PostConstruct
    void initStrategies() {
        strategiesByFormat = exportStrategies.stream()
                .collect(Collectors.toMap(ReportExportStrategy::getFormat, s -> s));
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    @Async("pdfExecutor")
    public void generateAsync(GeneratedReportEntity report) {
        try {
            switch (report.getReportType()) {
                case MONTHLY_FINES    -> generateMonthlyFinesReport(report);
                case OFFICER_ACTIVITY -> generateOfficerActivityReport(report);
                case ZONE_RANKING     -> generateZoneRankingReport(report);
                case DRIVER_HISTORY   -> generateDriverHistoryReport(report);
                //case CAMERA_UPTIME    -> generateCameraUptimeReport(report);
            }
        } catch (Exception ex) {
            log.error("Report generation failed for {}: {}", report.getId(), ex.getMessage(), ex);
            markFailed(report, ex.getMessage());
        }
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateMonthlyFinesReport(GeneratedReportEntity report) throws IOException {
        markGenerating(report);

        LocalDateTime from = report.getPeriodStart().atStartOfDay();
        LocalDateTime to   = report.getPeriodEnd().plusDays(1).atStartOfDay();

        List<Object[]> byType     = fineRepository.aggregateByViolationTypeInRange(from, to);
        int totalIssued            = fineRepository.countIssuedInRange(from, to);
        BigDecimal totalAmount     = fineRepository.sumAmountInRange(from, to);
        int totalOverdue           = fineRepository.countOverdueInRange(from, to);
        BigDecimal totalCollected  = fineRepository.sumCollectedInRange(from, to);

        List<String> headers = List.of("Violation Type", "Fines Issued", "Total Amount");
        List<List<String>> rows = new ArrayList<>();
        for (Object[] row : byType) {
            rows.add(List.of(String.valueOf(row[0]), String.valueOf(row[1]), row[2].toString()));
        }
        rows.add(List.of("TOTAL",     String.valueOf(totalIssued), totalAmount.toString()));
        rows.add(List.of("OVERDUE",   String.valueOf(totalOverdue), "-"));
        rows.add(List.of("COLLECTED", "-", totalCollected.toString()));

        String filePath = buildFilePath(report);
        writeReport(report, filePath, "Monthly Fines Report", headers, rows);
        markDone(report, filePath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateOfficerActivityReport(GeneratedReportEntity report) throws IOException {
        markGenerating(report);

        LocalDateTime from = report.getPeriodStart().atStartOfDay();
        LocalDateTime to   = report.getPeriodEnd().plusDays(1).atStartOfDay();

        List<Object[]> byOfficer = violationRepository.countByOfficerInRange(from, to);

        List<String> headers = List.of("Officer", "Violations Recorded");
        List<List<String>> rows = byOfficer.stream()
                .map(row -> List.of(String.valueOf(row[1]), String.valueOf(row[2])))
                .map(l -> (List<String>) l)
                .toList();

        String filePath = buildFilePath(report);
        writeReport(report, filePath, "Officer Activity Report", headers, rows);
        markDone(report, filePath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateZoneRankingReport(GeneratedReportEntity report) throws IOException {
        markGenerating(report);

        List<AccidentHotspotEntity> hotspots =
                analyticsRepository.findTop10BySeverityScoreDescAndPeriodEnd(report.getPeriodEnd());

        List<String> headers = List.of("Location", "Violation Count", "Dominant Type", "Severity Score");
        List<List<String>> rows = hotspots.stream()
                .map(h -> List.of(
                        h.getLocationLabel() != null ? h.getLocationLabel()
                                : h.getLatitude() + ", " + h.getLongitude(),
                        String.valueOf(h.getViolationCount()),
                        h.getDominantType() != null ? h.getDominantType() : "-",
                        String.valueOf(h.getSeverityScore())))
                .toList();

        String filePath = buildFilePath(report);
        writeReport(report, filePath, "Zone Ranking Report", headers, rows);
        markDone(report, filePath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateDriverHistoryReport(GeneratedReportEntity report) throws IOException {
        markGenerating(report);

        UUID driverId = extractDriverId(report.getParameters());
        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver " + driverId + " not found"));

        List<ViolationDto> violations            = violationService.getViolationsForDriver(driverId);
        List<FineDto> fines                      = fineService.getForDriver(driverId);
        List<AppealDto> appeals                  = appealService.getForDriver(driverId);
        List<LicenseSuspensionDto> suspensions   = driverService.getSuspensionHistory(driverId);
        List<DriverPointHistoryDto> pointHistory = driverService.getPointHistory(driverId);

        String filePath = buildFilePath(report);
        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());

        strategyFor(report.getFormat())
                .writeDriverHistoryReport(path, driver, violations, fines, appeals, suspensions, pointHistory);

        markDone(report, filePath);
    }

    private UUID extractDriverId(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank()) {
            throw new BadRequestException("Driver history report requires a driverId parameter");
        }
        try {
            JsonNode node = objectMapper.readTree(parametersJson);
            JsonNode driverIdNode = node.get("driverId");
            if (driverIdNode == null || driverIdNode.isNull()) {
                throw new BadRequestException("Driver history report requires a driverId parameter");
            }
            return UUID.fromString(driverIdNode.asText());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new BadRequestException("Invalid or malformed driverId in report parameters: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void markGenerating(GeneratedReportEntity report) {
        report.setStatus(ReportStatus.GENERATING);
        reportRepository.save(report);
    }

    private void markDone(GeneratedReportEntity report, String filePath) {
        report.setStatus(ReportStatus.DONE);
        report.setFilePath(filePath);
        report.setCompletedAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    private void markFailed(GeneratedReportEntity report, String errorMessage) {
        report.setStatus(ReportStatus.FAILED);
        report.setErrorMessage(errorMessage);
        report.setCompletedAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    private String buildFilePath(GeneratedReportEntity report) {
        String ext = report.getFormat() == ReportFormat.PDF ? "pdf" : "csv";
        return outputDir + "/reports/" + report.getId() + "." + ext;
    }

    private void writeReport(GeneratedReportEntity report, String filePath, String title,
                             List<String> headers, List<List<String>> rows) throws IOException {
        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());
        strategyFor(report.getFormat()).writeTabularReport(path, title, headers, rows);
    }

    private ReportExportStrategy strategyFor(ReportFormat format) {
        ReportExportStrategy strategy = strategiesByFormat.get(format);
        if (strategy == null) {
            throw new BadRequestException("No export strategy registered for format " + format);
        }
        return strategy;
    }
}
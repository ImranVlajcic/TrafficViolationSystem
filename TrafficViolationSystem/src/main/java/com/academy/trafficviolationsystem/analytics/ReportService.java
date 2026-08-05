package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseService;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for report management.
 *
 * Implements BaseService (not BaseCRUDService) — reports are never created
 * via a generic insert(). Creation goes through requestReport() which
 * validates the request and fires the async generation pipeline.
 *
 * Key operations:
 *   requestReport()   — creates PENDING row, fires async generation
 *   getReport()       — poll status (returns ReportDto, never the file)
 *   getReportEntity() — internal: loads entity with filePath for streaming
 *   getMyReports()    — scoped to the current authenticated user
 *   search()          — paginated search for admins
 */
@Service
@Transactional
public class ReportService implements BaseService<
        GeneratedReportEntity, ReportDto, ReportSearchObject, UUID> {

    private final GeneratedReportRepository reportRepository;
    private final ReportGenerationService   generationService;
    private final UserRepository            userRepository;
    private final ReportMapper              reportMapper;
    private final EntityManager             entityManager;

    public ReportService(GeneratedReportRepository reportRepository,
                          ReportGenerationService generationService,
                          UserRepository userRepository,
                          ReportMapper reportMapper,
                          EntityManager entityManager) {
        this.reportRepository  = reportRepository;
        this.generationService = generationService;
        this.userRepository    = userRepository;
        this.reportMapper      = reportMapper;
        this.entityManager     = entityManager;
    }

    // ── BaseService wiring ────────────────────────────────────────────────────

    @Override public CrudRepository<GeneratedReportEntity, UUID> getRepository()    { return reportRepository; }
    @Override public EntityManager                               getEntityManager() { return entityManager;    }
    @Override public ReportMapper                                getMapper()        { return reportMapper;     }
    @Override public Class<GeneratedReportEntity>                getEntityClass()   { return GeneratedReportEntity.class; }

    // ── search filters ────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                             ReportSearchObject searchObj,
                                             Root<GeneratedReportEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), searchObj.getStatus()));
        }
        if (searchObj.getReportType() != null) {
            predicates.add(cb.equal(root.get("reportType"), searchObj.getReportType()));
        }
        if (searchObj.getFormat() != null) {
            predicates.add(cb.equal(root.get("format"), searchObj.getFormat()));
        }
        if (searchObj.getRequestedById() != null) {
            predicates.add(cb.equal(
                root.get("requestedBy").get("id"), searchObj.getRequestedById()));
        }
        if (searchObj.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("created"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                root.get("created"), searchObj.getToDate().plusDays(1).atStartOfDay()));
        }
        return predicates;
    }

    // ── report creation ───────────────────────────────────────────────────────

    /**
     * Creates a PENDING report row and fires async generation.
     * Returns immediately with the PENDING ReportDto — client polls
     * GET /api/reports/{id} until isReady = true.
     *
     * @param principal Authenticated user requesting the report.
     */
    @Transactional
    @AuditAction(value = "REQUEST_REPORT", entityClass = GeneratedReportEntity.class)
    public ReportDto requestReport(ReportRequestDto request, UserPrincipal principal) {
        UserEntity requester = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User " + principal.getId() + " not found"));

        GeneratedReportEntity report = GeneratedReportEntity.builder()
                .reportType(request.getReportType())
                .format(request.getFormat())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .parameters(request.getParameters())
                .status(ReportStatus.PENDING)
                .requestedBy(requester)
                .build();

        report = reportRepository.save(report);

        // Fire async generation — runs on pdfExecutor, does not block
        generationService.generateAsync(report);

        return reportMapper.toDto(report);
    }

    // ── read operations ───────────────────────────────────────────────────────

    /**
     * Returns a ReportDto (always JSON, never the file).
     * Client polls this until isReady = true, then calls the /download endpoint.
     */
    @Transactional(readOnly = true)
    public ReportDto getReport(UUID reportId) {
        return reportMapper.toDto(getReportEntity(reportId));
    }

    /**
     * Returns the raw entity — used internally by ReportController to access
     * filePath for streaming. filePath is excluded from ReportDto intentionally.
     */
    @Transactional(readOnly = true)
    public GeneratedReportEntity getReportEntity(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report " + reportId + " not found"));
    }

    /**
     * Returns the authenticated user's own report history, newest first.
     * Used by GET /api/reports/my — citizens and officers only see their own.
     */
    @Transactional(readOnly = true)
    public List<ReportDto> getMyReports(UserPrincipal principal) {
        return reportRepository
                .findByRequestedByIdOrderByCreatedDesc(principal.getId())
                .stream()
                .map(reportMapper::toDto)
                .collect(Collectors.toList());
    }
}

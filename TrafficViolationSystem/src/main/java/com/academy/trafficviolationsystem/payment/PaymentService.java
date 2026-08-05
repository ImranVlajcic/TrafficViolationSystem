package com.academy.trafficviolationsystem.payment;

import com.academy.trafficviolationsystem.audit.AuditAction;
import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.payment.PaymentAlreadyProcessedException;
import com.academy.trafficviolationsystem.core.security.UserPrincipal;
import com.academy.trafficviolationsystem.core.services.BaseService;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.fine.FineRepository;
import com.academy.trafficviolationsystem.fine.FineService;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for the payment domain.
 *
 * Implements BaseService (not BaseCRUDService) — payment creation flows
 * through pay() which calls the gateway simulator, not a generic insert().
 *
 * Key operations:
 *   pay(request, principal)           — main payment flow
 *   getPaymentsForFine(fineId)        — all attempts for a fine
 *   toDtoWithFineNumber(payment)      — enriches DTO with fine number
 *
 * Cross-module calls (payment/ → fine/):
 *   fineService.markPaid(fineId)      — called after SUCCESS
 *   fineRepository.findById(fineId)   — to read fine details and totalDue
 *
 * The direction payment/ → fine/ is fine because fine/ does not import
 * payment/. Fines do not need to know about individual payment transactions.
 */
@Service
@Transactional
public class PaymentService implements BaseService<PaymentEntity, PaymentDto, PaymentSearchObject, UUID> {

    private final PaymentRepository              paymentRepository;
    private final PaymentMapper                  paymentMapper;
    //private final PaymentGatewaySimulator        gatewaySimulator;
    private final PaymentGatewayAdapter paymentGateway;
    private final PaymentConfirmationPdfService  pdfService;
    private final FineRepository                 fineRepository;
    private final FineService                    fineService;
    private final UserRepository                 userRepository;
    private final EntityManager                  entityManager;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentMapper paymentMapper,
                          PaymentGatewaySimulator gatewaySimulator, PaymentGatewayAdapter paymentGateway,
                          PaymentConfirmationPdfService pdfService,
                          FineRepository fineRepository,
                          FineService fineService,
                          UserRepository userRepository,
                          EntityManager entityManager) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper     = paymentMapper;
        this.paymentGateway = paymentGateway;
        //this.gatewaySimulator  = gatewaySimulator;
        this.pdfService        = pdfService;
        this.fineRepository    = fineRepository;
        this.fineService       = fineService;
        this.userRepository    = userRepository;
        this.entityManager     = entityManager;
    }

    // ── BaseService wiring ────────────────────────────────────────────────

    @Override public CrudRepository<PaymentEntity, UUID> getRepository()    { return paymentRepository; }
    @Override public EntityManager                       getEntityManager() { return entityManager;     }
    @Override public PaymentMapper                       getMapper()        { return paymentMapper;     }
    @Override public Class<PaymentEntity>                getEntityClass()   { return PaymentEntity.class; }

    // ── main payment flow ─────────────────────────────────────────────────

    /**
     * Initiates and processes a payment against a fine.
     *
     * Flow:
     *  1. Load and validate the fine (must be UNPAID or OVERDUE).
     *  2. Idempotency: generate transactionId, check it doesn't already exist.
     *  3. Call PaymentGatewaySimulator.simulate().
     *  4. Persist PaymentEntity with the gateway outcome.
     *  5. If SUCCESS: call fineService.markPaid() + trigger async receipt PDF.
     *  6. Return PaymentDto with enriched fineNumber.
     *
     * @param request   The payment request from the client.
     * @param principal The authenticated user initiating the payment.
     * @return PaymentDto reflecting the gateway outcome.
     */
    @Transactional
    @AuditAction(value = "PAY_FINE", entityClass = PaymentEntity.class)
    public PaymentDto pay(PaymentRequest request, UserPrincipal principal) {

        // 0. Idempotency check — must happen before any gateway call or DB write
        Optional<PaymentEntity> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            throw new PaymentAlreadyProcessedException(existing.get().getTransactionId());
        }

        // 1. Load and validate fine
        FineEntity fine = fineRepository.findById(request.getFineId())
                .orElseThrow(() -> new NotFoundException("Fine " + request.getFineId() + " not found"));

        validateFineIsPayable(fine);

        // 2. Generate transaction ID (now purely a display/reference number,
        //    not the idempotency mechanism)
        String transactionId = generateTransactionId();

        // 3. Call the gateway simulator
        //SimulationResult result = gatewaySimulator.simulate(request, fine.getTotalDue());
        SimulationResult result = paymentGateway.processPayment(request, fine.getTotalDue());

        // 4. Load payer entity
        UserEntity paidBy = userRepository.findById(principal.getId()).orElse(null);

        // 5. Build and persist PaymentEntity
        PaymentEntity payment = buildPaymentEntity(request, fine, paidBy, transactionId, result);
        payment = paymentRepository.save(payment);

        // 6. Post-success side effects
        if (result.status() == PaymentStatus.SUCCESS) {
            afterSuccessfulPayment(payment, fine);
        }

        return toDtoWithFineNumber(payment);
    }

    // ── search ────────────────────────────────────────────────────────────

    @Override
    public List<Predicate> additionalFilter(CriteriaBuilder cb,
                                            PaymentSearchObject searchObj,
                                            Root<PaymentEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchObj.getFineId() != null) {
            predicates.add(cb.equal(root.get("fine").get("id"), searchObj.getFineId()));
        }
        if (searchObj.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), searchObj.getStatus()));
        }
        if (searchObj.getMethod() != null) {
            predicates.add(cb.equal(root.get("method"), searchObj.getMethod()));
        }
        if (searchObj.getPaidById() != null) {
            predicates.add(cb.equal(root.get("paidBy").get("id"), searchObj.getPaidById()));
        }
        if (searchObj.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("paidAt"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                root.get("paidAt"), searchObj.getToDate().plusDays(1).atStartOfDay()));
        }
        return predicates;
    }

    // ── read helpers ──────────────────────────────────────────────────────

    /**
     * All payment attempts for a specific fine, newest first.
     * Used by FineController and PaymentController.
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsForFine(UUID fineId) {
        return paymentRepository.findByFineIdOrderByCreatedDesc(fineId)
                .stream()
                .map(this::toDtoWithFineNumber)
                .toList();
    }

    /**
     * Enriches a PaymentDto with the fine's human-readable fineNumber.
     * MapStruct cannot do this inline because it requires a separate repository
     * call — done here instead.
     */
    @Transactional(readOnly = true)
    public PaymentDto toDtoWithFineNumber(PaymentEntity payment) {
        PaymentDto dto = paymentMapper.toDto(payment);
        if (payment.getFine() != null) {
            dto.setFineNumber(payment.getFine().getFineNumber());
        }
        return dto;
    }

    // ── private helpers ───────────────────────────────────────────────────

    private void validateFineIsPayable(FineEntity fine) {
        switch (fine.getStatus()) {
            case PAID -> throw new AppException(HttpStatus.CONFLICT,
                ErrorCode.FINE_ALREADY_PAID,
                "Fine " + fine.getFineNumber() + " has already been paid");

            case CANCELLED -> throw new AppException(HttpStatus.CONFLICT,
                ErrorCode.FINE_CANCELLED,
                "Fine " + fine.getFineNumber() + " has been cancelled and cannot be paid");

            case DISPUTED -> throw new AppException(HttpStatus.CONFLICT,
                ErrorCode.BAD_REQUEST,
                "Fine " + fine.getFineNumber() + " is currently under appeal — payment is suspended until the appeal is resolved");

            case UNPAID, OVERDUE -> { /* payable — proceed */ }
        }
    }

    private void afterSuccessfulPayment(PaymentEntity payment, FineEntity fine) {
        // Mark the fine as PAID and close the associated violation
        fineService.markPaid(fine.getId());

        // Trigger async receipt PDF generation — runs on pdfExecutor, does not block
        pdfService.generateReceipt(payment, fine);
    }

    private PaymentEntity buildPaymentEntity(PaymentRequest request,
                                              FineEntity fine,
                                              UserEntity paidBy,
                                              String transactionId,
                                              SimulationResult result) {
        PaymentEntity payment = new PaymentEntity();
        payment.setTransactionId(transactionId);
        payment.setFine(fine);
        payment.setPaidBy(paidBy);
        payment.setAmount(fine.getTotalDue());
        payment.setCurrency(fine.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setStatus(result.status());
        payment.setGatewayResponse(result.responseJson());
        payment.setNotes(result.status() == PaymentStatus.FAILED
                ? result.message()
                : request.getNotes());

        if (result.status() == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now());
        }
        return payment;
    }

    /**
     * Generates a unique transaction ID in format TXN-{YYYYMMDD}-{6-digit-seq}.
     * e.g. TXN-20250601-000042
     */
    private String generateTransactionId() {
        String today    = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd   = yearStart.plusYears(1);
        long count = paymentRepository.countByYear(yearStart, yearEnd);
        return String.format("TXN-%s-%06d", today, count + 1);
    }
}

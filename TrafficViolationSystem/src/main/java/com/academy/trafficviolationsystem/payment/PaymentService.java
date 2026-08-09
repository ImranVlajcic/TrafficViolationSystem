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
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.concurrent.ThreadLocalRandom;

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
    private final PaymentGatewayAdapter paymentGateway;
    private final FineRepository                 fineRepository;
    private final FineService                    fineService;
    private final UserRepository                 userRepository;
    private final EntityManager                  entityManager;
    private final ApplicationEventPublisher       eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentMapper paymentMapper,
                          PaymentGatewayAdapter paymentGateway,
                          FineRepository fineRepository,
                          FineService fineService,
                          UserRepository userRepository,
                          EntityManager entityManager,
                          ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper     = paymentMapper;
        this.paymentGateway = paymentGateway;
        this.fineRepository    = fineRepository;
        this.fineService       = fineService;
        this.userRepository    = userRepository;
        this.entityManager     = entityManager;
        this.eventPublisher    = eventPublisher;
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
     *  3. Call PaymentGatewayAdapter.processPayment().
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
                root.get("created"), searchObj.getFromDate().atStartOfDay()));
        }
        if (searchObj.getToDate() != null) {
            predicates.add(cb.lessThan(
                root.get("created"), searchObj.getToDate().plusDays(1).atStartOfDay()));
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

        // Defer receipt PDF generation until this transaction actually commits.
        // PaymentConfirmationPdfService is @Async and would otherwise start on a
        // separate thread before the payment row is durable, causing its update
        // to silently match zero rows. See PaymentSucceededEvent for details.
        eventPublisher.publishEvent(new PaymentSucceededEvent(payment, fine));
    }

    private PaymentEntity buildPaymentEntity(PaymentRequest request,
                                              FineEntity fine,
                                              UserEntity paidBy,
                                              String transactionId,
                                              SimulationResult result) {
        PaymentEntity payment = new PaymentEntity();
        payment.setTransactionId(transactionId);
        payment.setIdempotencyKey(request.getIdempotencyKey());
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
     * Generates a unique transaction ID in format TXN-{YYYYMMDD}-{6-digit}.
     * e.g. TXN-20250601-004217
     *
     * Previously this counted existing rows for the year and used count+1 as
     * the suffix. That is not safe under concurrency: two payments processed
     * at the same moment can both read the same count before either commits,
     * producing the same transactionId and colliding on uk_payment_txn_id.
     *
     * Instead we draw a random 6-digit suffix and check it's unused, retrying
     * a few times on the rare chance of a collision. The uk_payment_txn_id
     * unique constraint remains the authoritative guard — if two requests
     * somehow race past the existsBy check with the same candidate, the DB
     * save() will throw and the caller sees a normal transaction failure
     * rather than two payments silently sharing a reference number.
     */
    private String generateTransactionId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = String.format(
                    "TXN-%s-%06d", today, ThreadLocalRandom.current().nextInt(0, 1_000_000));
            if (!paymentRepository.existsByTransactionId(candidate)) {
                return candidate;
            }
        }

        // Exceedingly unlikely fallback: guarantee uniqueness via UUID.
        return "TXN-" + today + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

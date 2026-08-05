package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.fine.FineStatus;
import com.academy.trafficviolationsystem.payment.PaymentEntity;
import com.academy.trafficviolationsystem.payment.PaymentMethod;
import com.academy.trafficviolationsystem.payment.PaymentRepository;
import com.academy.trafficviolationsystem.payment.PaymentStatus;
import com.academy.trafficviolationsystem.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds payment rows for every PAID fine (one SUCCESS row, ~1/3 of those
 * also get a preceding FAILED attempt to exercise the retry-payment UI),
 * plus 2 REFUNDED rows against CANCELLED fines that were paid just before
 * their appeal was approved — a real edge case worth having in test data.
 */
@Component
public class PaymentSeeder {

    private final PaymentRepository paymentRepository;

    public PaymentSeeder(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentEntity> seed(List<FineEntity> fines) {
        if (paymentRepository.count() > 0) return paymentRepository.findAll();

        List<PaymentEntity> created = new ArrayList<>();
        int txnSeq = 1;
        int refundsAssigned = 0;

        List<FineEntity> paidFines = new ArrayList<>();
        List<FineEntity> cancelledFines = new ArrayList<>();
        for (FineEntity fine : fines) {
            if (fine.getStatus() == FineStatus.PAID) paidFines.add(fine);
            if (fine.getStatus() == FineStatus.CANCELLED) cancelledFines.add(fine);
        }

        for (FineEntity fine : paidFines) {
            LocalDateTime paidAt = fine.getPaidAt() != null ? fine.getPaidAt() : fine.getIssuedAt().plusDays(3);

            if (SeedRandom.chance(0.33)) {
                created.add(save(fine, paidAt.minusHours(SeedRandom.intBetween(1, 20)),
                        PaymentStatus.FAILED, "Kartica odbijena — nedovoljno sredstava", txnSeq));
                txnSeq++;
            }
            created.add(save(fine, paidAt, PaymentStatus.SUCCESS, null, txnSeq));
            txnSeq++;
        }

        for (FineEntity fine : cancelledFines) {
            if (refundsAssigned >= 2) break;
            if (!SeedRandom.chance(0.5)) continue;
            LocalDateTime paidAt = fine.getIssuedAt().plusDays(SeedRandom.intBetween(1, 10));
            created.add(save(fine, paidAt, PaymentStatus.SUCCESS, null, txnSeq));
            txnSeq++;
            created.add(save(fine, paidAt.plusDays(SeedRandom.intBetween(2, 15)), PaymentStatus.REFUNDED,
                    "Povrat sredstava nakon usvojene žalbe", txnSeq));
            txnSeq++;
            refundsAssigned++;
        }

        return created;
    }

    private PaymentEntity save(FineEntity fine, LocalDateTime when, PaymentStatus status, String notes, int txnSeq) {
        PaymentEntity payment = new PaymentEntity();
        String yyyymmdd = when.toLocalDate().toString().replace("-", "");
        payment.setTransactionId(String.format("TXN-%s-%06d", yyyymmdd, txnSeq));
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setAmount(fine.getTotalDue() != null ? fine.getTotalDue() : fine.getAmount());
        payment.setCurrency(SeedConstants.DEFAULT_CURRENCY);
        payment.setMethod(weightedMethod());
        payment.setStatus(status);
        payment.setPaidAt(status == PaymentStatus.SUCCESS ? when : null);
        payment.setGatewayResponse("{\"result\":\"" + status.name() + "\",\"simulated\":true}");
        payment.setNotes(notes);
        payment.setFine(fine);
        UserEntity payer = fine.getDriver() != null ? fine.getDriver().getUser() : null;
        payment.setPaidBy(payer);
        return paymentRepository.save(payment);
    }

    private PaymentMethod weightedMethod() {
        double r = SeedRandom.RNG.nextDouble();
        if (r < 0.4) return PaymentMethod.CREDIT_CARD;
        if (r < 0.6) return PaymentMethod.DEBIT_CARD;
        if (r < 0.8) return PaymentMethod.ONLINE_PORTAL;
        if (r < 0.92) return PaymentMethod.BANK_TRANSFER;
        return PaymentMethod.CASH;
    }
}
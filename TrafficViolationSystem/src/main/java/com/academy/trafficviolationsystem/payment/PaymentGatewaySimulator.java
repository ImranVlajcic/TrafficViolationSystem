package com.academy.trafficviolationsystem.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulates an external payment gateway for the internship project.
 *
 * In a production system this class would be replaced by an HTTP client
 * calling a real gateway (Stripe, PayPal, Monri, etc.). The interface
 * would remain the same — only the implementation changes.
 *
 * Simulation rules (deterministic — no randomness, makes tests predictable):
 *
 *   notes ends with "FAIL"       → FAILED  (test failure scenario)
 *   notes ends with "LIMIT"      → FAILED  "Daily limit exceeded"
 *   amount > 5000 BAM            → FAILED  "Amount exceeds daily limit"
 *   method == CASH               → SUCCESS (cash is always accepted)
 *   method == BANK_TRANSFER      → SUCCESS (bank transfers always succeed)
 *   all other cases              → SUCCESS
 *
 * Testing tips:
 *   To test a failure: send notes = "TEST_FAIL"
 *   To test a limit:   send amount > 5000 or notes ending "LIMIT"
 *   To test success:   any normal request
 */
@Component
public class PaymentGatewaySimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaySimulator.class);
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("5000.00");

    /**
     * Simulates submitting a payment to an external gateway.
     *
     * @param request The original payment request from the client.
     * @param amount  The amount to charge (taken from fine.totalDue by PaymentService).
     * @return SimulationResult with the gateway outcome.
     */
    public SimulationResult simulate(PaymentRequest request, BigDecimal amount) {
        String gatewayTxnId = "GW-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        log.debug("Gateway simulation: method={}, amount={}", request.getMethod(), amount);

        // Rule 1: explicit test failure trigger via notes field
        if (request.getNotes() != null && request.getNotes().toUpperCase().endsWith("FAIL")) {
            log.debug("Gateway simulation → FAILED (test trigger in notes)");
            return SimulationResult.failed(gatewayTxnId, "Payment declined by gateway (test trigger)");
        }

        // Rule 2: explicit daily limit trigger
        if (request.getNotes() != null && request.getNotes().toUpperCase().endsWith("LIMIT")) {
            log.debug("Gateway simulation → FAILED (limit trigger in notes)");
            return SimulationResult.failed(gatewayTxnId, "Daily payment limit exceeded");
        }

        // Rule 3: amount exceeds configured daily limit
        if (amount.compareTo(DAILY_LIMIT) > 0) {
            log.debug("Gateway simulation → FAILED (amount {} exceeds daily limit {})", amount, DAILY_LIMIT);
            return SimulationResult.failed(gatewayTxnId,
                "Amount " + amount + " BAM exceeds the single-transaction limit of " + DAILY_LIMIT + " BAM");
        }

        // Rule 4: cash and bank transfers always succeed
        if (request.getMethod() == PaymentMethod.CASH
                || request.getMethod() == PaymentMethod.BANK_TRANSFER) {
            log.debug("Gateway simulation → SUCCESS ({})", request.getMethod());
            return SimulationResult.success(gatewayTxnId);
        }

        // Rule 5: all other card/online payments succeed
        log.debug("Gateway simulation → SUCCESS (card/online)");
        return SimulationResult.success(gatewayTxnId);
    }
}

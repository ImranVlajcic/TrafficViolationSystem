package com.academy.trafficviolationsystem.payment;

/**
 * The outcome of a simulated payment gateway call.
 *
 * Returned by PaymentGatewaySimulator.simulate() and consumed by
 * PaymentService to decide how to update the PaymentEntity.
 *
 * @param status             The outcome: SUCCESS, FAILED, or REFUNDED.
 * @param gatewayTransactionId A unique ID the simulated gateway assigns to the transaction.
 * @param message            Human-readable explanation (stored in PaymentEntity.notes on failure).
 * @param responseJson       The raw JSON string to store in PaymentEntity.gatewayResponse.
 */
public record SimulationResult(
        PaymentStatus status,
        String gatewayTransactionId,
        String message,
        String responseJson
) {

    /** Factory for a successful result. */
    public static SimulationResult success(String gatewayTxnId) {
        String json = String.format(
            "{\"status\":\"SUCCESS\",\"gatewayTxnId\":\"%s\",\"message\":\"Payment approved\"}", 
            gatewayTxnId);
        return new SimulationResult(PaymentStatus.SUCCESS, gatewayTxnId, "Payment approved", json);
    }

    /** Factory for a failed result with a specific reason. */
    public static SimulationResult failed(String gatewayTxnId, String reason) {
        String json = String.format(
            "{\"status\":\"FAILED\",\"gatewayTxnId\":\"%s\",\"message\":\"%s\"}", 
            gatewayTxnId, reason);
        return new SimulationResult(PaymentStatus.FAILED, gatewayTxnId, reason, json);
    }
}

package com.academy.trafficviolationsystem.core.exceptions.fine;

/**
 * Thrown when an operation (payment) is attempted on a fine that is currently
 * DISPUTED — i.e. an appeal has been filed and is awaiting a decision.
 *
 * NOTE: This file was written without visibility into your actual
 * core.exceptions.fine package (FineAlreadyPaidException, FineCancelledException,
 * etc. were not included in the uploaded module). It mirrors their apparent
 * single-String-arg constructor pattern, but you'll need to:
 *   1. Move this into your core module's exceptions.fine package.
 *   2. Align it with your real base exception class (the one that presumably
 *      carries an ErrorCode, per your structured exception hierarchy).
 *   3. Add a corresponding ErrorCode entry (e.g. FINE_DISPUTED) if your
 *      exception hierarchy requires one, the same way FINE_ALREADY_PAID /
 *      FINE_CANCELLED presumably already do.
 */
public class FineDisputedException extends RuntimeException {

    public FineDisputedException(String fineNumber) {
        super("Fine " + fineNumber + " is currently under dispute and cannot be paid " +
                "until the appeal is resolved.");
    }
}

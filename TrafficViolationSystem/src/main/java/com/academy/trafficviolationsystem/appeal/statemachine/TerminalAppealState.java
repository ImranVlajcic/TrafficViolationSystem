package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.core.exceptions.appeal.InvalidAppealStatusException;

/**
 * Base for the three terminal statuses: APPROVED, REJECTED, WITHDRAWN.
 *
 * No transition is legal from a terminal state. {@code startReview} and
 * {@code withdraw} already fall back to {@link AppealState}'s defaults.
 * {@code approve}/{@code reject} both funnel through {@link #decisionAlreadyMade},
 * matching the original behaviour where re-deciding an already-decided
 * appeal gives the same message regardless of which action was attempted.
 */
abstract class TerminalAppealState implements AppealState {

    @Override
    public final AppealState approve(String appealNumber) {
        throw decisionAlreadyMade(appealNumber);
    }

    @Override
    public final AppealState reject(String appealNumber) {
        throw decisionAlreadyMade(appealNumber);
    }

    protected abstract InvalidAppealStatusException decisionAlreadyMade(String appealNumber);
}

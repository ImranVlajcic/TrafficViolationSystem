package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;

/**
 * Appeal filed, awaiting officer assignment. The only editable state,
 * and the only one from which every transition is legal:
 * an officer can pick it up, approve or reject it directly, or the
 * driver can withdraw it.
 */
final class SubmittedState implements AppealState {

    static final SubmittedState INSTANCE = new SubmittedState();

    private SubmittedState() {}

    @Override
    public AppealStatus getStatus() {
        return AppealStatus.SUBMITTED;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public AppealState startReview(String appealNumber) {
        return UnderReviewState.INSTANCE;
    }

    @Override
    public AppealState approve(String appealNumber) {
        return ApprovedState.INSTANCE;
    }

    @Override
    public AppealState reject(String appealNumber) {
        return RejectedState.INSTANCE;
    }

    @Override
    public AppealState withdraw(String appealNumber) {
        return WithdrawnState.INSTANCE;
    }
}

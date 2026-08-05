package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;

/**
 * An officer has picked up the appeal. It can still be approved or
 * rejected, but it can no longer be handed to another officer
 * (startReview) or withdrawn by the driver — both fall back to
 * {@link AppealState}'s default "illegal transition" behaviour.
 */
final class UnderReviewState implements AppealState {

    static final UnderReviewState INSTANCE = new UnderReviewState();

    private UnderReviewState() {}

    @Override
    public AppealStatus getStatus() {
        return AppealStatus.UNDER_REVIEW;
    }

    @Override
    public AppealState approve(String appealNumber) {
        return ApprovedState.INSTANCE;
    }

    @Override
    public AppealState reject(String appealNumber) {
        return RejectedState.INSTANCE;
    }
}

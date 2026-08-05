package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;

/**
 * Resolves an {@link AppealStatus} to its {@link AppealState} singleton.
 *
 * This is the single point of contact between the plain persistence enum
 * (stored on the entity, used in queries/DTOs) and the behaviour-carrying
 * state classes. Nothing else in the appeal package should switch on
 * AppealStatus to decide what's legal — that logic belongs in the states.
 */
public final class AppealStates {

    private AppealStates() {}

    public static AppealState of(AppealStatus status) {
        return switch (status) {
            case SUBMITTED -> SubmittedState.INSTANCE;
            case UNDER_REVIEW -> UnderReviewState.INSTANCE;
            case APPROVED -> ApprovedState.INSTANCE;
            case REJECTED -> RejectedState.INSTANCE;
            case WITHDRAWN -> WithdrawnState.INSTANCE;
        };
    }
}

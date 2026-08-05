package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;
import com.academy.trafficviolationsystem.core.exceptions.appeal.InvalidAppealStatusException;

/** Officer approved the appeal. Terminal. */
final class ApprovedState extends TerminalAppealState {

    static final ApprovedState INSTANCE = new ApprovedState();

    private ApprovedState() {}

    @Override
    public AppealStatus getStatus() {
        return AppealStatus.APPROVED;
    }

    @Override
    protected InvalidAppealStatusException decisionAlreadyMade(String appealNumber) {
        return new InvalidAppealStatusException(
                "Appeal " + appealNumber + " has already been approved");
    }
}

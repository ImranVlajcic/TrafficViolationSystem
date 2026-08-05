package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;
import com.academy.trafficviolationsystem.core.exceptions.appeal.InvalidAppealStatusException;

/** Officer rejected the appeal. Terminal. */
final class RejectedState extends TerminalAppealState {

    static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {}

    @Override
    public AppealStatus getStatus() {
        return AppealStatus.REJECTED;
    }

    @Override
    protected InvalidAppealStatusException decisionAlreadyMade(String appealNumber) {
        return new InvalidAppealStatusException(
                "Appeal " + appealNumber + " has already been rejected");
    }
}

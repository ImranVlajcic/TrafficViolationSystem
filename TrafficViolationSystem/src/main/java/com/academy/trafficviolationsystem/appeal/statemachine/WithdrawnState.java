package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;
import com.academy.trafficviolationsystem.core.exceptions.appeal.InvalidAppealStatusException;

/** Driver withdrew the appeal before a decision. Terminal. */
final class WithdrawnState extends TerminalAppealState {

    static final WithdrawnState INSTANCE = new WithdrawnState();

    private WithdrawnState() {}

    @Override
    public AppealStatus getStatus() {
        return AppealStatus.WITHDRAWN;
    }

    @Override
    protected InvalidAppealStatusException decisionAlreadyMade(String appealNumber) {
        return new InvalidAppealStatusException(
                "Appeal " + appealNumber + " has been withdrawn");
    }
}

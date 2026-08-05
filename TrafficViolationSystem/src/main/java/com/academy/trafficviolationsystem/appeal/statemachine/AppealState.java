package com.academy.trafficviolationsystem.appeal.statemachine;

import com.academy.trafficviolationsystem.appeal.AppealStatus;
import com.academy.trafficviolationsystem.core.exceptions.appeal.InvalidAppealStatusException;

/**
 * A single node in the appeal status state machine.
 *
 * Each {@link AppealStatus} has exactly one implementation, resolved via
 * {@link AppealStates#of(AppealStatus)}. A state exposes the transitions
 * legal from it; illegal transitions throw {@link InvalidAppealStatusException}.
 *
 * States are stateless singletons — they only decide "is this move legal,
 * and if so what's the next status". They never touch the entity, other
 * services, or persistence; AppealService still owns all of that. This
 * keeps the state machine testable in isolation from Spring/JPA.
 *
 * {@code startReview} and {@code withdraw} share the same illegal-transition
 * wording across every non-SUBMITTED status, so those defaults live here.
 * {@code approve} and {@code reject} have per-terminal-status wording
 * (see {@link TerminalAppealState}) and are declared abstract.
 */
public interface AppealState {

    AppealStatus getStatus();

    /** Only SUBMITTED appeals can be edited (reason/evidence). */
    default boolean isEditable() {
        return false;
    }

    default AppealState startReview(String appealNumber) {
        throw new InvalidAppealStatusException(
                "Appeal " + appealNumber + " is already " + getStatus());
    }

    default AppealState withdraw(String appealNumber) {
        throw new InvalidAppealStatusException(
                "Appeal " + appealNumber +
                        " can only be withdrawn while in SUBMITTED status. Current status: " + getStatus());
    }

    AppealState approve(String appealNumber);

    AppealState reject(String appealNumber);
}

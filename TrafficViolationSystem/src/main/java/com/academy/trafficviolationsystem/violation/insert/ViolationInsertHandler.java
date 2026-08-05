package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.violation.ViolationCreateRequest;
import com.academy.trafficviolationsystem.violation.ViolationEntity;

/**
 * One link in the beforeInsert validation/hydration chain.
 *
 * Each handler does its own work in doHandle() — hydrating a relationship,
 * validating a field, deriving a flag — then, if it hasn't thrown, passes
 * control to the next handler. A handler that wants to short-circuit the
 * rest of the chain simply throws (exactly like the original inline checks
 * did); nothing downstream runs, and the exception propagates up through
 * BaseCRUDService.insert() unchanged.
 *
 * Handlers are wired into a fixed order by ViolationInsertChain, matching
 * the original beforeInsert() sequence exactly:
 *   reference number -> vehicle -> driver -> speeding check -> status/flag
 */
public abstract class ViolationInsertHandler {

    private ViolationInsertHandler next;

    /** Links this handler to the next one in the chain; returns next so calls can be chained fluently. */
    public ViolationInsertHandler linkWith(ViolationInsertHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(ViolationCreateRequest request, ViolationEntity entity) {
        doHandle(request, entity);
        if (next != null) {
            next.handle(request, entity);
        }
    }

    protected abstract void doHandle(ViolationCreateRequest request, ViolationEntity entity);
}

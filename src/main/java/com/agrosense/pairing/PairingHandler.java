package com.agrosense.pairing;

import com.agrosense.model.PairingCode;

import java.util.Optional;

/**
 * Abstract base for Chain of Responsibility pairing validation handlers.
 */
public abstract class PairingHandler {
    protected PairingHandler next;

    public PairingHandler setNext(PairingHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Validate the pairing code. Return a failure result immediately on any violation,
     * or delegate to the next handler.
     *
     * @param code     the raw pairing code string entered by the customer
     * @param resolved the resolved PairingCode entity (may be empty at first handler)
     */
    public abstract PairingResult handle(String code, Optional<PairingCode> resolved);

    protected PairingResult passToNext(String code, Optional<PairingCode> resolved) {
        if (next != null) return next.handle(code, resolved);
        return PairingResult.ok();
    }
}

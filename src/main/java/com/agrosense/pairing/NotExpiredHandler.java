package com.agrosense.pairing;

import com.agrosense.model.PairingCode;
import com.agrosense.model.PairingCodeStatus;

import java.util.Optional;

/**
 * Handler 2: Verifies the pairing code has not expired.
 */
public class NotExpiredHandler extends PairingHandler {

    @Override
    public PairingResult handle(String code, Optional<PairingCode> resolved) {
        PairingCode pc = resolved.orElseThrow();
        if (pc.getStatus() == PairingCodeStatus.EXPIRED || pc.isExpired()) {
            return PairingResult.fail("This pairing code has expired. Please contact support for a new code.");
        }
        return passToNext(code, resolved);
    }
}

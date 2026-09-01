package com.agrosense.pairing;

import com.agrosense.model.PairingCode;
import com.agrosense.model.PairingCodeStatus;

import java.util.Optional;

/**
 * Handler 3: Verifies the pairing code has not already been used or revoked.
 */
public class NotAlreadyUsedHandler extends PairingHandler {

    @Override
    public PairingResult handle(String code, Optional<PairingCode> resolved) {
        PairingCode pc = resolved.orElseThrow();
        if (pc.getStatus() == PairingCodeStatus.ACTIVE) {
            return PairingResult.fail("This pairing code has already been used to pair a device.");
        }
        if (pc.getStatus() == PairingCodeStatus.REVOKED) {
            return PairingResult.fail("This pairing code has been revoked. Please contact support.");
        }
        return passToNext(code, resolved);
    }
}

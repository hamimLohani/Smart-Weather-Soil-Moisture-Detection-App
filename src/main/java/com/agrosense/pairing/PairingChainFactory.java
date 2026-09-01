package com.agrosense.pairing;

import com.agrosense.dao.DeviceUnitDAO;
import com.agrosense.dao.PairingCodeDAO;

/**
 * Factory that builds the pairing validation chain in order:
 * CodeExists → NotExpired → NotAlreadyUsed → DeviceAvailable
 */
public class PairingChainFactory {

    public static PairingHandler build(PairingCodeDAO pairingCodeDAO, DeviceUnitDAO deviceUnitDAO) {
        PairingHandler head = new CodeExistsHandler(pairingCodeDAO);
        head.setNext(new NotExpiredHandler())
            .setNext(new NotAlreadyUsedHandler())
            .setNext(new DeviceAvailableHandler(deviceUnitDAO));
        return head;
    }
}

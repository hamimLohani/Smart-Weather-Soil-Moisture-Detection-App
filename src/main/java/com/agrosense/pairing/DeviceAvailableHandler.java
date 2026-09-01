package com.agrosense.pairing;

import com.agrosense.dao.DeviceUnitDAO;
import com.agrosense.model.DeviceStatus;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.PairingCode;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Handler 4 (final): Verifies the linked DeviceUnit is available for pairing
 * (status must be UNPAIRED, not PAIRED or FAULTY).
 */
public class DeviceAvailableHandler extends PairingHandler {

    private final DeviceUnitDAO deviceUnitDAO;

    public DeviceAvailableHandler(DeviceUnitDAO deviceUnitDAO) {
        this.deviceUnitDAO = deviceUnitDAO;
    }

    @Override
    public PairingResult handle(String code, Optional<PairingCode> resolved) {
        PairingCode pc = resolved.orElseThrow();
        try {
            Optional<DeviceUnit> unitOpt = deviceUnitDAO.findById(pc.getDeviceUnitId());
            if (unitOpt.isEmpty()) {
                return PairingResult.fail("Device associated with this code could not be found.");
            }
            DeviceUnit unit = unitOpt.get();
            if (unit.getStatus() == DeviceStatus.FAULTY) {
                return PairingResult.fail("This device is marked as faulty and cannot be paired.");
            }
            if (unit.getStatus() == DeviceStatus.PAIRED) {
                return PairingResult.fail("This device is already paired to another account.");
            }
            return passToNext(code, resolved);
        } catch (SQLException e) {
            return PairingResult.fail("Database error while checking device availability: " + e.getMessage());
        }
    }
}

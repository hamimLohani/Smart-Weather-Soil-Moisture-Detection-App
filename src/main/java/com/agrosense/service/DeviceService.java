package com.agrosense.service;

import com.agrosense.dao.DevicePairingDAO;
import com.agrosense.dao.DeviceUnitDAO;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.DevicePairing;
import com.agrosense.state.PairedState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages device lookup and unpairing for the customer.
 */
public class DeviceService {

    private final DeviceUnitDAO deviceUnitDAO;
    private final DevicePairingDAO devicePairingDAO;

    public DeviceService(DeviceUnitDAO deviceUnitDAO, DevicePairingDAO devicePairingDAO) {
        this.deviceUnitDAO = deviceUnitDAO;
        this.devicePairingDAO = devicePairingDAO;
    }

    /** Returns all devices actively paired to the given customer. */
    public List<DeviceUnit> getDevicesForCustomer(int customerId) throws SQLException {
        List<DevicePairing> pairings = devicePairingDAO.findByCustomer(customerId);
        List<DeviceUnit> devices = new ArrayList<>();
        for (DevicePairing p : pairings) {
            if (p.isActive()) {
                deviceUnitDAO.findById(p.getDeviceUnitId()).ifPresent(devices::add);
            }
        }
        return devices;
    }

    /** Returns all pairings (active and historical) for the customer. */
    public List<DevicePairing> getPairingsForCustomer(int customerId) throws SQLException {
        return devicePairingDAO.findByCustomer(customerId);
    }

    public Optional<DeviceUnit> getDeviceById(int id) throws SQLException {
        return deviceUnitDAO.findById(id);
    }

    public Optional<DeviceUnit> getDeviceBySerial(String serial) throws SQLException {
        return deviceUnitDAO.findBySerialNumber(serial);
    }

    /**
     * Unpairs a device: deactivates the DevicePairing record and transitions
     * DeviceUnit state PAIRED → UNPAIRED.
     */
    public void unpairDevice(int devicePairingId) throws SQLException {
        DevicePairing pairing = devicePairingDAO.findById(devicePairingId).orElseThrow();
        devicePairingDAO.deactivate(devicePairingId);
        var state = new PairedState();
        var next = state.unpair();
        deviceUnitDAO.updateStatus(pairing.getDeviceUnitId(), next.getStatus());
    }

    /** Returns the active pairing for a device, if any. */
    public Optional<DevicePairing> getActivePairing(int deviceUnitId) throws SQLException {
        return devicePairingDAO.findActiveByDeviceUnit(deviceUnitId);
    }
}

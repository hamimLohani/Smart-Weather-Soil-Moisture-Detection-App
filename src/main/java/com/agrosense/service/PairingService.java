package com.agrosense.service;

import com.agrosense.dao.DevicePairingDAO;
import com.agrosense.dao.DeviceUnitDAO;
import com.agrosense.dao.PairingCodeDAO;
import com.agrosense.dao.SiteDAO;
import com.agrosense.model.*;
import com.agrosense.pairing.*;
import com.agrosense.state.PairedState;
import com.agrosense.state.UnpairedState;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Orchestrates device pairing: runs the Chain of Responsibility validation,
 * then on success: creates Site (if needed), DevicePairing record,
 * transitions DeviceUnit state to PAIRED, and marks the PairingCode ACTIVE.
 */
public class PairingService {

    private final PairingCodeDAO pairingCodeDAO;
    private final DeviceUnitDAO deviceUnitDAO;
    private final DevicePairingDAO devicePairingDAO;
    private final SiteDAO siteDAO;
    private final PairingHandler validationChain;

    public PairingService(PairingCodeDAO pairingCodeDAO, DeviceUnitDAO deviceUnitDAO,
                          DevicePairingDAO devicePairingDAO, SiteDAO siteDAO) {
        this.pairingCodeDAO = pairingCodeDAO;
        this.deviceUnitDAO = deviceUnitDAO;
        this.devicePairingDAO = devicePairingDAO;
        this.siteDAO = siteDAO;
        this.validationChain = PairingChainFactory.build(pairingCodeDAO, deviceUnitDAO);
    }

    /**
     * Validates the pairing code and, if valid, creates all necessary records.
     *
     * @param customerId  logged-in customer
     * @param code        raw pairing code string entered by user
     * @param siteName    site name entered by user
     * @param profile     HOME or FARM
     * @return PairingResult with success flag and failure reason on error
     */
    public PairingResult pair(int customerId, String code, String siteName,
                              UseCaseProfile profile) throws SQLException {
        // Run the chain
        PairingResult result = validationChain.handle(code.trim().toUpperCase(), Optional.empty());
        if (!result.isSuccess()) return result;

        // Retrieve validated entities
        PairingCode pairingCode = pairingCodeDAO.findByCode(code.trim().toUpperCase()).orElseThrow();
        DeviceUnit device = deviceUnitDAO.findById(pairingCode.getDeviceUnitId()).orElseThrow();

        // Create Site
        Site site = siteDAO.insert(customerId, siteName, profile);

        // Create DevicePairing record
        devicePairingDAO.insert(device.getId(), customerId, site.getId());

        // State transition: UNPAIRED → PAIRED
        var state = new UnpairedState();
        var nextState = state.pair();
        deviceUnitDAO.updateStatus(device.getId(), nextState.getStatus());

        // Mark pairing code as ACTIVE
        pairingCodeDAO.markActive(pairingCode.getId());

        return PairingResult.ok();
    }
}

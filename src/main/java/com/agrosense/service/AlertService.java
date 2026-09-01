package com.agrosense.service;

import com.agrosense.dao.*;
import com.agrosense.model.*;
import com.agrosense.observer.AlertObserver;
import com.agrosense.state.*;
import com.agrosense.strategy.*;

import java.sql.SQLException;
import java.util.*;

/**
 * Central alert service.
 * - Evaluates active AlertRules against incoming sensor readings
 * - Picks the correct Strategy (HOME vs FARM)
 * - Notifies all registered Observers
 * - Handles AlertEvent state transitions (acknowledge / resolve)
 */
public class AlertService {

    private final AlertRuleDAO alertRuleDAO;
    private final AlertEventDAO alertEventDAO;
    private final DevicePairingDAO devicePairingDAO;
    private final SiteDAO siteDAO;

    private final List<AlertObserver> observers = new ArrayList<>();
    private final Set<Integer> sitesNeedingAttention = new HashSet<>();

    public AlertService(AlertRuleDAO alertRuleDAO, AlertEventDAO alertEventDAO,
                        DevicePairingDAO devicePairingDAO, SiteDAO siteDAO) {
        this.alertRuleDAO = alertRuleDAO;
        this.alertEventDAO = alertEventDAO;
        this.devicePairingDAO = devicePairingDAO;
        this.siteDAO = siteDAO;
    }

    public void addObserver(AlertObserver observer) {
        observers.add(observer);
    }

    public Set<Integer> getSitesNeedingAttention() {
        return Collections.unmodifiableSet(sitesNeedingAttention);
    }

    public void clearSiteAttention(int siteId) {
        sitesNeedingAttention.remove(siteId);
    }

    /**
     * Evaluates all active AlertRules for the device's site against the provided readings.
     * Triggers Strategy + Observer chain for each breached rule.
     */
    public void evaluate(DeviceUnit device, List<SensorReading> readings) throws SQLException {
        Optional<DevicePairing> pairingOpt = devicePairingDAO.findActiveByDeviceUnit(device.getId());
        if (pairingOpt.isEmpty()) return;

        DevicePairing pairing = pairingOpt.get();
        Optional<Site> siteOpt = siteDAO.findById(pairing.getSiteId());
        if (siteOpt.isEmpty()) return;

        Site site = siteOpt.get();
        List<AlertRule> rules = alertRuleDAO.findActiveBySite(site.getId());

        for (SensorReading reading : readings) {
            for (AlertRule rule : rules) {
                if (rule.getReadingType() == reading.getReadingType()
                        && rule.isTriggered(reading.getValue())) {
                    AlertEvent event = alertEventDAO.insert(
                        device.getId(), rule.getId(), reading.getValue());
                    // Pick strategy based on use-case profile
                    AlertResponseStrategy strategy = selectStrategy(site);
                    strategy.respond(event, device, site, reading);
                }
            }
        }
    }

    /** Acknowledge an alert event (State pattern: OPEN → ACKNOWLEDGED). */
    public void acknowledge(int eventId) throws SQLException {
        AlertEvent event = alertEventDAO.findById(eventId).orElseThrow();
        AlertEventState state = stateFor(event.getStatus());
        AlertEventState next = state.acknowledge();
        alertEventDAO.updateStatus(eventId, next.getStatus());
    }

    /** Resolve an alert event (State pattern: OPEN|ACK → RESOLVED). */
    public void resolve(int eventId) throws SQLException {
        AlertEvent event = alertEventDAO.findById(eventId).orElseThrow();
        AlertEventState state = stateFor(event.getStatus());
        AlertEventState next = state.resolve();
        alertEventDAO.updateStatus(eventId, next.getStatus());
    }

    public List<AlertEvent> getEventsForCustomer(int customerId) throws SQLException {
        return alertEventDAO.findByCustomer(customerId);
    }

    public List<AlertEvent> getEventsForCustomerByStatus(int customerId,
                                                          AlertEventStatus status) throws SQLException {
        return alertEventDAO.findByCustomerAndStatus(customerId, status);
    }

    public int countOpenAlerts(int deviceUnitId) throws SQLException {
        return alertEventDAO.countOpenByDevice(deviceUnitId);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private AlertResponseStrategy selectStrategy(Site site) {
        return switch (site.getUseCaseProfile()) {
            case FARM -> new FarmAlertStrategy(observers, sitesNeedingAttention);
            case HOME -> new HomeAlertStrategy(observers);
        };
    }

    private AlertEventState stateFor(AlertEventStatus status) {
        return switch (status) {
            case OPEN         -> new OpenState();
            case ACKNOWLEDGED -> new AcknowledgedState();
            case RESOLVED     -> new ResolvedState();
        };
    }
}

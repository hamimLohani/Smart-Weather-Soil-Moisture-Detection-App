package com.agrosense.observer;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.Site;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Observer that logs alert events to the JVM logger (customer-visible history).
 * The AlertEventDAO also persists the record — this observer provides a runtime log.
 */
public class LogAlertObserver implements AlertObserver {

    private static final Logger LOG = Logger.getLogger(LogAlertObserver.class.getName());

    @Override
    public void onAlert(AlertEvent event, DeviceUnit device, Site site) {
        LOG.warning(String.format(
            "[ALERT] id=%d | device=%s | site=%s | value=%.2f | rule=%d | time=%s",
            event.getId(),
            device.getSerialNumber(),
            site.getName(),
            event.getTriggeredValue(),
            event.getAlertRuleId(),
            LocalDateTime.now()
        ));
    }
}

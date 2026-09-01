package com.agrosense.strategy;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.SensorReading;
import com.agrosense.model.Site;

/**
 * Strategy interface for alert response behaviour, varying by Site use-case profile.
 */
public interface AlertResponseStrategy {
    void respond(AlertEvent event, DeviceUnit device, Site site, SensorReading reading);
}

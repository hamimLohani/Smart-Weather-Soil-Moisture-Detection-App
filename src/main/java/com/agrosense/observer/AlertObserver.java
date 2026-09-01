package com.agrosense.observer;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.Site;

/**
 * Observer interface for alert notifications.
 */
public interface AlertObserver {
    void onAlert(AlertEvent event, DeviceUnit device, Site site);
}

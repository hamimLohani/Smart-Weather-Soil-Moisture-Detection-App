package com.agrosense.strategy;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.SensorReading;
import com.agrosense.model.Site;
import com.agrosense.observer.AlertObserver;

import java.util.List;
import java.util.Set;

/**
 * FARM profile strategy: creates AlertEvent + notification + adds site to a shared
 * "sites needing attention" set so the Dashboard can badge FARM sites.
 */
public class FarmAlertStrategy implements AlertResponseStrategy {

    private final List<AlertObserver> observers;
    private final Set<Integer> sitesNeedingAttention; // shared with DashboardController

    public FarmAlertStrategy(List<AlertObserver> observers, Set<Integer> sitesNeedingAttention) {
        this.observers = observers;
        this.sitesNeedingAttention = sitesNeedingAttention;
    }

    @Override
    public void respond(AlertEvent event, DeviceUnit device, Site site, SensorReading reading) {
        System.out.printf("[FarmAlertStrategy] Alert for FARM site '%s' on device %s — %s = %.2f%n",
            site.getName(), device.getSerialNumber(),
            reading.getReadingType(), reading.getValue());

        // Add to aggregated "sites needing attention" for the Dashboard badge
        sitesNeedingAttention.add(site.getId());

        // Notify all observers
        for (AlertObserver observer : observers) {
            observer.onAlert(event, device, site);
        }
    }
}

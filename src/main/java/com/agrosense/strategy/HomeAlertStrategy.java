package com.agrosense.strategy;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.SensorReading;
import com.agrosense.model.Site;
import com.agrosense.observer.AlertObserver;

import java.util.List;

/**
 * HOME profile strategy: creates an AlertEvent and sends a single simple notification.
 */
public class HomeAlertStrategy implements AlertResponseStrategy {

    private final List<AlertObserver> observers;

    public HomeAlertStrategy(List<AlertObserver> observers) {
        this.observers = observers;
    }

    @Override
    public void respond(AlertEvent event, DeviceUnit device, Site site, SensorReading reading) {
        System.out.printf("[HomeAlertStrategy] Alert for HOME site '%s' on device %s — %s = %.2f%n",
            site.getName(), device.getSerialNumber(),
            reading.getReadingType(), reading.getValue());
        // Notify all observers (Dashboard update + log)
        for (AlertObserver observer : observers) {
            observer.onAlert(event, device, site);
        }
    }
}

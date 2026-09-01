package com.agrosense.observer;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.DeviceUnit;
import com.agrosense.model.Site;
import javafx.application.Platform;
import javafx.collections.ObservableList;

/**
 * Observer that pushes new alert events into an ObservableList
 * that the Dashboard screen binds to. Updates are marshalled to the FX thread.
 */
public class DashboardAlertObserver implements AlertObserver {

    private final ObservableList<AlertEvent> liveAlerts;

    public DashboardAlertObserver(ObservableList<AlertEvent> liveAlerts) {
        this.liveAlerts = liveAlerts;
    }

    @Override
    public void onAlert(AlertEvent event, DeviceUnit device, Site site) {
        Platform.runLater(() -> liveAlerts.add(0, event));
    }
}

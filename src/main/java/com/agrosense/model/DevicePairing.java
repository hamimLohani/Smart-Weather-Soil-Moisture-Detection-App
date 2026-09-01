package com.agrosense.model;

import java.time.LocalDateTime;

public class DevicePairing {
    private int id;
    private int deviceUnitId;
    private int customerId;
    private int siteId;
    private LocalDateTime pairedDate;
    private LocalDateTime unpairedDate;
    private boolean active;

    public DevicePairing() {}

    public DevicePairing(int id, int deviceUnitId, int customerId, int siteId,
                         LocalDateTime pairedDate, LocalDateTime unpairedDate, boolean active) {
        this.id = id;
        this.deviceUnitId = deviceUnitId;
        this.customerId = customerId;
        this.siteId = siteId;
        this.pairedDate = pairedDate;
        this.unpairedDate = unpairedDate;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeviceUnitId() { return deviceUnitId; }
    public void setDeviceUnitId(int deviceUnitId) { this.deviceUnitId = deviceUnitId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getSiteId() { return siteId; }
    public void setSiteId(int siteId) { this.siteId = siteId; }

    public LocalDateTime getPairedDate() { return pairedDate; }
    public void setPairedDate(LocalDateTime pairedDate) { this.pairedDate = pairedDate; }

    public LocalDateTime getUnpairedDate() { return unpairedDate; }
    public void setUnpairedDate(LocalDateTime unpairedDate) { this.unpairedDate = unpairedDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

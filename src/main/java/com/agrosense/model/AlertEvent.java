package com.agrosense.model;

import java.time.LocalDateTime;

public class AlertEvent {
    private int id;
    private int deviceUnitId;
    private int alertRuleId;
    private double triggeredValue;
    private AlertEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public AlertEvent() {}

    public AlertEvent(int id, int deviceUnitId, int alertRuleId, double triggeredValue,
                      AlertEventStatus status, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.deviceUnitId = deviceUnitId;
        this.alertRuleId = alertRuleId;
        this.triggeredValue = triggeredValue;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeviceUnitId() { return deviceUnitId; }
    public void setDeviceUnitId(int deviceUnitId) { this.deviceUnitId = deviceUnitId; }

    public int getAlertRuleId() { return alertRuleId; }
    public void setAlertRuleId(int alertRuleId) { this.alertRuleId = alertRuleId; }

    public double getTriggeredValue() { return triggeredValue; }
    public void setTriggeredValue(double triggeredValue) { this.triggeredValue = triggeredValue; }

    public AlertEventStatus getStatus() { return status; }
    public void setStatus(AlertEventStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}

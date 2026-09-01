package com.agrosense.model;

import java.time.LocalDateTime;

public class DeviceUnit {
    private int id;
    private String serialNumber;
    private int productId;
    private DeviceStatus status;
    private int dryCalibrationValue;
    private int wetCalibrationValue;
    private LocalDateTime lastSeen;

    public DeviceUnit() {}

    public DeviceUnit(int id, String serialNumber, int productId, DeviceStatus status,
                      int dryCalibrationValue, int wetCalibrationValue, LocalDateTime lastSeen) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.productId = productId;
        this.status = status;
        this.dryCalibrationValue = dryCalibrationValue;
        this.wetCalibrationValue = wetCalibrationValue;
        this.lastSeen = lastSeen;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }

    public int getDryCalibrationValue() { return dryCalibrationValue; }
    public void setDryCalibrationValue(int dryCalibrationValue) { this.dryCalibrationValue = dryCalibrationValue; }

    public int getWetCalibrationValue() { return wetCalibrationValue; }
    public void setWetCalibrationValue(int wetCalibrationValue) { this.wetCalibrationValue = wetCalibrationValue; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    /**
     * Converts a raw ADC soil moisture reading to percentage (0–100%).
     * dry = fully dry sensor reading (higher ADC = drier for capacitive sensors)
     * wet = fully wet sensor reading (lower ADC)
     */
    public double convertSoilMoistureToPercent(int rawValue) {
        if (dryCalibrationValue == wetCalibrationValue) return 0.0;
        double percent = ((double)(dryCalibrationValue - rawValue) / (dryCalibrationValue - wetCalibrationValue)) * 100.0;
        return Math.max(0.0, Math.min(100.0, percent));
    }

    @Override
    public String toString() { return serialNumber + " [" + status + "]"; }
}

package com.agrosense.model;

import java.time.LocalDateTime;

public class SensorReading {
    private int id;
    private int deviceUnitId;
    private ReadingType readingType;
    private double value;
    private LocalDateTime timestamp;

    public SensorReading() {}

    public SensorReading(int id, int deviceUnitId, ReadingType readingType, double value, LocalDateTime timestamp) {
        this.id = id;
        this.deviceUnitId = deviceUnitId;
        this.readingType = readingType;
        this.value = value;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeviceUnitId() { return deviceUnitId; }
    public void setDeviceUnitId(int deviceUnitId) { this.deviceUnitId = deviceUnitId; }

    public ReadingType getReadingType() { return readingType; }
    public void setReadingType(ReadingType readingType) { this.readingType = readingType; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

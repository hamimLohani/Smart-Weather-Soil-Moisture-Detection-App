package com.agrosense.service;

import com.agrosense.dao.SensorReadingDAO;
import com.agrosense.model.ReadingType;
import com.agrosense.model.SensorReading;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SensorReadingService {

    private final SensorReadingDAO sensorReadingDAO;

    public SensorReadingService(SensorReadingDAO sensorReadingDAO) {
        this.sensorReadingDAO = sensorReadingDAO;
    }

    public void saveReading(int deviceUnitId, ReadingType type, double value) throws SQLException {
        sensorReadingDAO.insert(deviceUnitId, type, value);
    }

    public Optional<SensorReading> getLatest(int deviceUnitId, ReadingType type) throws SQLException {
        return sensorReadingDAO.findLatest(deviceUnitId, type);
    }
    
    public boolean isOnline(int deviceUnitId) {
        try {
            return getLatest(deviceUnitId, ReadingType.TEMPERATURE)
                .map(r -> r.getTimestamp() != null && r.getTimestamp().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(2)))
                .orElse(false);
        } catch (SQLException e) {
            return false;
        }
    }

    public List<SensorReading> getHistory(int deviceUnitId, ReadingType type,
                                          LocalDateTime from, LocalDateTime to) throws SQLException {
        return sensorReadingDAO.findByDeviceAndTypeAndRange(deviceUnitId, type, from, to);
    }

    public List<SensorReading> getAllHistory(int deviceUnitId,
                                             LocalDateTime from, LocalDateTime to) throws SQLException {
        return sensorReadingDAO.findByDeviceAndRange(deviceUnitId, from, to);
    }
}

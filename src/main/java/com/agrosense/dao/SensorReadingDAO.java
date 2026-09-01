package com.agrosense.dao;

import com.agrosense.model.SensorReading;
import com.agrosense.model.ReadingType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SensorReadingDAO {

    public void insert(int deviceUnitId, ReadingType type, double value) throws SQLException {
        String sql = "INSERT INTO SensorReading(device_unit_id, reading_type, value) VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            ps.setString(2, type.name());
            ps.setDouble(3, value);
            ps.executeUpdate();
        }
    }

    /**
     * Returns the latest reading of a given type for a device, if any.
     */
    public Optional<SensorReading> findLatest(int deviceUnitId, ReadingType type) throws SQLException {
        String sql = "SELECT id, device_unit_id, reading_type, value, timestamp " +
                     "FROM SensorReading WHERE device_unit_id=? AND reading_type=? " +
                     "ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns readings for a device and type within a date range, ordered ascending for charting.
     * Uses the (device_unit_id, timestamp) index.
     */
    public List<SensorReading> findByDeviceAndTypeAndRange(
            int deviceUnitId, ReadingType type,
            LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT id, device_unit_id, reading_type, value, timestamp " +
                     "FROM SensorReading " +
                     "WHERE device_unit_id=? AND reading_type=? " +
                     "  AND timestamp >= ? AND timestamp <= ? " +
                     "ORDER BY timestamp ASC";
        List<SensorReading> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            ps.setString(2, type.name());
            ps.setString(3, from.toString().replace("T", " "));
            ps.setString(4, to.toString().replace("T", " "));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** All readings for a device (all types) in a date range — used for combined charts. */
    public List<SensorReading> findByDeviceAndRange(
            int deviceUnitId, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT id, device_unit_id, reading_type, value, timestamp " +
                     "FROM SensorReading " +
                     "WHERE device_unit_id=? AND timestamp >= ? AND timestamp <= ? " +
                     "ORDER BY timestamp ASC";
        List<SensorReading> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            ps.setString(2, from.toString().replace("T", " "));
            ps.setString(3, to.toString().replace("T", " "));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private SensorReading map(ResultSet rs) throws SQLException {
        String ts = rs.getString("timestamp");
        LocalDateTime timestamp = ts != null ? LocalDateTime.parse(ts.replace(" ", "T")) : null;
        return new SensorReading(
            rs.getInt("id"),
            rs.getInt("device_unit_id"),
            ReadingType.valueOf(rs.getString("reading_type")),
            rs.getDouble("value"),
            timestamp
        );
    }
}

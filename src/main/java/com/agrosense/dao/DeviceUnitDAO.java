package com.agrosense.dao;

import com.agrosense.model.DeviceUnit;
import com.agrosense.model.DeviceStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeviceUnitDAO {

    public Optional<DeviceUnit> findById(int id) throws SQLException {
        String sql = "SELECT id, serial_number, product_id, status, " +
                     "dry_calibration_value, wet_calibration_value, last_seen " +
                     "FROM DeviceUnit WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<DeviceUnit> findBySerialNumber(String serialNumber) throws SQLException {
        String sql = "SELECT id, serial_number, product_id, status, " +
                     "dry_calibration_value, wet_calibration_value, last_seen " +
                     "FROM DeviceUnit WHERE serial_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<DeviceUnit> findByStatus(DeviceStatus status) throws SQLException {
        String sql = "SELECT id, serial_number, product_id, status, " +
                     "dry_calibration_value, wet_calibration_value, last_seen " +
                     "FROM DeviceUnit WHERE status = ?";
        List<DeviceUnit> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void updateStatus(int id, DeviceStatus status) throws SQLException {
        String sql = "UPDATE DeviceUnit SET status=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateLastSeen(int id) throws SQLException {
        String sql = "UPDATE DeviceUnit SET last_seen=DATETIME('now', 'localtime') WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int insert(String serialNumber) throws SQLException {
        String sql = "INSERT INTO DeviceUnit(serial_number, product_id, status, dry_calibration_value, wet_calibration_value) " +
                     "VALUES(?, COALESCE((SELECT id FROM Product LIMIT 1), 1), 'UNPAIRED', 820, 380)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, serialNumber);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to insert new DeviceUnit");
    }

    private DeviceUnit map(ResultSet rs) throws SQLException {
        String ts = rs.getString("last_seen");
        LocalDateTime lastSeen = ts != null ? LocalDateTime.parse(ts.replace(" ", "T")) : null;
        return new DeviceUnit(
            rs.getInt("id"),
            rs.getString("serial_number"),
            rs.getInt("product_id"),
            DeviceStatus.valueOf(rs.getString("status")),
            rs.getInt("dry_calibration_value"),
            rs.getInt("wet_calibration_value"),
            lastSeen
        );
    }
}

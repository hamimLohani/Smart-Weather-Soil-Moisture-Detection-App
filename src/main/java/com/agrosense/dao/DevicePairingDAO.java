package com.agrosense.dao;

import com.agrosense.model.DevicePairing;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DevicePairingDAO {

    public DevicePairing insert(int deviceUnitId, int customerId, int siteId) throws SQLException {
        String sql = "INSERT INTO DevicePairing(device_unit_id, customer_id, site_id) VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, deviceUnitId);
            ps.setInt(2, customerId);
            ps.setInt(3, siteId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getInt(1)).orElseThrow();
            }
        }
        throw new SQLException("Insert failed, no key returned.");
    }

    public Optional<DevicePairing> findById(int id) throws SQLException {
        String sql = "SELECT id, device_unit_id, customer_id, site_id, " +
                     "paired_date, unpaired_date, is_active FROM DevicePairing WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<DevicePairing> findActiveByDeviceUnit(int deviceUnitId) throws SQLException {
        String sql = "SELECT id, device_unit_id, customer_id, site_id, " +
                     "paired_date, unpaired_date, is_active " +
                     "FROM DevicePairing WHERE device_unit_id=? AND is_active=1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<DevicePairing> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT id, device_unit_id, customer_id, site_id, " +
                     "paired_date, unpaired_date, is_active " +
                     "FROM DevicePairing WHERE customer_id=? ORDER BY paired_date DESC";
        List<DevicePairing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<DevicePairing> findActiveBySite(int siteId) throws SQLException {
        String sql = "SELECT id, device_unit_id, customer_id, site_id, " +
                     "paired_date, unpaired_date, is_active " +
                     "FROM DevicePairing WHERE site_id=? AND is_active=1";
        List<DevicePairing> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void deactivate(int id) throws SQLException {
        String sql = "UPDATE DevicePairing SET is_active=0, unpaired_date=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private DevicePairing map(ResultSet rs) throws SQLException {
        String pd = rs.getString("paired_date");
        String ud = rs.getString("unpaired_date");
        return new DevicePairing(
            rs.getInt("id"),
            rs.getInt("device_unit_id"),
            rs.getInt("customer_id"),
            rs.getInt("site_id"),
            pd != null ? LocalDateTime.parse(pd.replace(" ", "T")) : null,
            ud != null ? LocalDateTime.parse(ud.replace(" ", "T")) : null,
            rs.getInt("is_active") == 1
        );
    }
}

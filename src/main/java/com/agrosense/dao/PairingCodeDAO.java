package com.agrosense.dao;

import com.agrosense.model.PairingCode;
import com.agrosense.model.PairingCodeStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class PairingCodeDAO {

    public Optional<PairingCode> findByCode(String code) throws SQLException {
        String sql = "SELECT id, code, device_unit_id, status, generated_date, used_date " +
                     "FROM PairingCode WHERE code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public void markActive(int id) throws SQLException {
        String sql = "UPDATE PairingCode SET status='ACTIVE', used_date=CURRENT_DATE WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void markStatus(int id, PairingCodeStatus status) throws SQLException {
        String sql = "UPDATE PairingCode SET status=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void markStatusByDeviceUnitId(int deviceUnitId, PairingCodeStatus status) throws SQLException {
        String sql = "UPDATE PairingCode SET status=? WHERE device_unit_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, deviceUnitId);
            ps.executeUpdate();
        }
    }

    public void insert(String code, int deviceUnitId) throws SQLException {
        String sql = "INSERT INTO PairingCode(code, device_unit_id, status, generated_date) " +
                     "VALUES(?, ?, 'UNUSED', CURRENT_DATE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, deviceUnitId);
            ps.executeUpdate();
        }
    }

    private PairingCode map(ResultSet rs) throws SQLException {
        String usedStr = rs.getString("used_date");
        return new PairingCode(
            rs.getInt("id"),
            rs.getString("code"),
            rs.getInt("device_unit_id"),
            PairingCodeStatus.valueOf(rs.getString("status")),
            LocalDate.parse(rs.getString("generated_date")),
            usedStr != null ? LocalDate.parse(usedStr) : null
        );
    }
}

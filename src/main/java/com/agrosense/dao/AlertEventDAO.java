package com.agrosense.dao;

import com.agrosense.model.AlertEvent;
import com.agrosense.model.AlertEventStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertEventDAO {

    public AlertEvent insert(int deviceUnitId, int alertRuleId, double triggeredValue) throws SQLException {
        String sql = "INSERT INTO AlertEvent(device_unit_id, alert_rule_id, triggered_value) " +
                     "VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, deviceUnitId);
            ps.setInt(2, alertRuleId);
            ps.setDouble(3, triggeredValue);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getInt(1)).orElseThrow();
            }
        }
        throw new SQLException("Insert failed, no key returned.");
    }

    public Optional<AlertEvent> findById(int id) throws SQLException {
        String sql = "SELECT id, device_unit_id, alert_rule_id, triggered_value, " +
                     "status, created_at, resolved_at FROM AlertEvent WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<AlertEvent> findByDevice(int deviceUnitId) throws SQLException {
        String sql = "SELECT id, device_unit_id, alert_rule_id, triggered_value, " +
                     "status, created_at, resolved_at FROM AlertEvent " +
                     "WHERE device_unit_id=? ORDER BY created_at DESC";
        List<AlertEvent> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<AlertEvent> findByCustomerAndStatus(int customerId, AlertEventStatus status) throws SQLException {
        String sql = "SELECT ae.id, ae.device_unit_id, ae.alert_rule_id, ae.triggered_value, " +
                     "       ae.status, ae.created_at, ae.resolved_at " +
                     "FROM AlertEvent ae " +
                     "JOIN DevicePairing dp ON dp.device_unit_id = ae.device_unit_id " +
                     "WHERE dp.customer_id=? AND dp.is_active=1 AND ae.status=? " +
                     "ORDER BY ae.created_at DESC";
        List<AlertEvent> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<AlertEvent> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT ae.id, ae.device_unit_id, ae.alert_rule_id, ae.triggered_value, " +
                     "       ae.status, ae.created_at, ae.resolved_at " +
                     "FROM AlertEvent ae " +
                     "JOIN DevicePairing dp ON dp.device_unit_id = ae.device_unit_id " +
                     "WHERE dp.customer_id=? AND dp.is_active=1 " +
                     "ORDER BY ae.created_at DESC";
        List<AlertEvent> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public int countOpenByDevice(int deviceUnitId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AlertEvent WHERE device_unit_id=? AND status='OPEN'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceUnitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void updateStatus(int id, AlertEventStatus status) throws SQLException {
        if (status == AlertEventStatus.RESOLVED) {
            String sql = "UPDATE AlertEvent SET status=?, resolved_at=CURRENT_TIMESTAMP WHERE id=?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE AlertEvent SET status=? WHERE id=?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        }
    }

    private AlertEvent map(ResultSet rs) throws SQLException {
        String ca = rs.getString("created_at");
        String ra = rs.getString("resolved_at");
        return new AlertEvent(
            rs.getInt("id"),
            rs.getInt("device_unit_id"),
            rs.getInt("alert_rule_id"),
            rs.getDouble("triggered_value"),
            AlertEventStatus.valueOf(rs.getString("status")),
            ca != null ? LocalDateTime.parse(ca.replace(" ", "T")) : null,
            ra != null ? LocalDateTime.parse(ra.replace(" ", "T")) : null
        );
    }
}

package com.agrosense.dao;

import com.agrosense.model.AlertRule;
import com.agrosense.model.ComparisonOperator;
import com.agrosense.model.ReadingType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertRuleDAO {

    public AlertRule insert(int siteId, ReadingType type, ComparisonOperator op,
                            double threshold) throws SQLException {
        String sql = "INSERT INTO AlertRule(site_id, reading_type, comparison_operator, " +
                     "threshold_value, is_active) VALUES(?,?,?,?,1)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, siteId);
            ps.setString(2, type.name());
            ps.setString(3, op.getSymbol());
            ps.setDouble(4, threshold);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getInt(1)).orElseThrow();
            }
        }
        throw new SQLException("Insert failed, no key returned.");
    }

    public Optional<AlertRule> findById(int id) throws SQLException {
        String sql = "SELECT id, site_id, reading_type, comparison_operator, " +
                     "threshold_value, is_active FROM AlertRule WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<AlertRule> findBySite(int siteId) throws SQLException {
        String sql = "SELECT id, site_id, reading_type, comparison_operator, " +
                     "threshold_value, is_active FROM AlertRule WHERE site_id=? ORDER BY id";
        List<AlertRule> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<AlertRule> findActiveBySite(int siteId) throws SQLException {
        String sql = "SELECT id, site_id, reading_type, comparison_operator, " +
                     "threshold_value, is_active FROM AlertRule " +
                     "WHERE site_id=? AND is_active=1 ORDER BY id";
        List<AlertRule> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void update(AlertRule rule) throws SQLException {
        String sql = "UPDATE AlertRule SET reading_type=?, comparison_operator=?, " +
                     "threshold_value=?, is_active=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rule.getReadingType().name());
            ps.setString(2, rule.getComparisonOperator().getSymbol());
            ps.setDouble(3, rule.getThresholdValue());
            ps.setInt(4, rule.isActive() ? 1 : 0);
            ps.setInt(5, rule.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM AlertRule WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private AlertRule map(ResultSet rs) throws SQLException {
        return new AlertRule(
            rs.getInt("id"),
            rs.getInt("site_id"),
            ReadingType.valueOf(rs.getString("reading_type")),
            ComparisonOperator.fromSymbol(rs.getString("comparison_operator")),
            rs.getDouble("threshold_value"),
            rs.getInt("is_active") == 1
        );
    }
}

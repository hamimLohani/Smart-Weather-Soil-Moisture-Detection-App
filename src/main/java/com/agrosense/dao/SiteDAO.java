package com.agrosense.dao;

import com.agrosense.model.Site;
import com.agrosense.model.UseCaseProfile;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiteDAO {

    public Site insert(int customerId, String name, UseCaseProfile profile) throws SQLException {
        String sql = "INSERT INTO Site(customer_id, name, use_case_profile) VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setString(2, name);
            ps.setString(3, profile.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getInt(1)).orElseThrow();
            }
        }
        throw new SQLException("Insert failed, no key returned.");
    }

    public Optional<Site> findById(int id) throws SQLException {
        String sql = "SELECT id, customer_id, name, use_case_profile, created_at FROM Site WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<Site> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT id, customer_id, name, use_case_profile, created_at " +
                     "FROM Site WHERE customer_id = ? ORDER BY name";
        List<Site> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void update(Site site) throws SQLException {
        String sql = "UPDATE Site SET name=?, use_case_profile=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, site.getName());
            ps.setString(2, site.getUseCaseProfile().name());
            ps.setInt(3, site.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Delete AlertEvents for any AlertRule associated with this site
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM AlertEvent WHERE alert_rule_id IN (SELECT id FROM AlertRule WHERE site_id = ?)")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                // 2. Delete AlertRules associated with this site
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM AlertRule WHERE site_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                // 3. Delete DevicePairings associated with this site
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM DevicePairing WHERE site_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                // 4. Delete the Site
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Site WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private Site map(ResultSet rs) throws SQLException {
        String ts = rs.getString("created_at");
        LocalDateTime createdAt = ts != null ? LocalDateTime.parse(ts.replace(" ", "T")) : null;
        return new Site(
            rs.getInt("id"),
            rs.getInt("customer_id"),
            rs.getString("name"),
            UseCaseProfile.valueOf(rs.getString("use_case_profile")),
            createdAt
        );
    }
}

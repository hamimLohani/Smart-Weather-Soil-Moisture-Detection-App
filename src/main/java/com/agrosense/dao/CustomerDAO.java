package com.agrosense.dao;

import com.agrosense.model.Customer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAO {

    public Optional<Customer> findByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, phone, email, password_hash, salt, created_at " +
                     "FROM Customer WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Customer> findById(int id) throws SQLException {
        String sql = "SELECT id, name, phone, email, password_hash, salt, created_at " +
                     "FROM Customer WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT id, name, phone, email, password_hash, salt, created_at " +
                     "FROM Customer ORDER BY name";
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Customer insert(String name, String phone, String email,
                           String passwordHash, String salt) throws SQLException {
        String sql = "INSERT INTO Customer(name, phone, email, password_hash, salt) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.setString(5, salt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
        }
        throw new SQLException("Insert failed, no key returned.");
    }

    public void update(Customer c) throws SQLException {
        String sql = "UPDATE Customer SET name=?, phone=?, email=?, password_hash=?, salt=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPasswordHash());
            ps.setString(5, c.getSalt());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        String ts = rs.getString("created_at");
        LocalDateTime createdAt = ts != null ? LocalDateTime.parse(ts.replace(" ", "T")) : null;
        return new Customer(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("salt"),
            createdAt
        );
    }
}

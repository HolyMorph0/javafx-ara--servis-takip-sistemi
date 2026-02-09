package com.osman.vssfx.dao;

import com.osman.vssfx.db.Db;

import java.sql.*;

public class UserDAO {

    public record UserRow(long userId, long tenantId, String role, String status,
                          String email, String passwordHash) {}

    public UserRow findActiveByTenantAndEmail(long tenantId, String email) throws SQLException {
        String sql = """
            SELECT user_id, tenant_id, role, status, email, password_hash
            FROM users
            WHERE tenant_id = ? AND email = ?
            LIMIT 1
        """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, tenantId);
            ps.setString(2, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new UserRow(
                        rs.getLong("user_id"),
                        rs.getLong("tenant_id"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("email"),
                        rs.getString("password_hash")
                );
            }
        }
    }

    public void updateLastLogin(long userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = NOW() WHERE user_id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}

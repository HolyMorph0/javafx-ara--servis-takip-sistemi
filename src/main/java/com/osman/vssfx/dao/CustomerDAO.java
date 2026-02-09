package com.osman.vssfx.dao;

import com.osman.vssfx.model.Customer;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<Customer> findAllByTenant(long tenantId) throws Exception {
        String sql = """
                SELECT customer_id, tenant_id, first_name, last_name, phone, email, user_id, created_at
                FROM customers
                WHERE tenant_id = ?
                ORDER BY customer_id DESC
                """;

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, tenantId);

            try (var rs = ps.executeQuery()) {
                List<Customer> out = new ArrayList<>();
                while (rs.next()) {
                    Customer c = new Customer();
                    c.setCustomerId(rs.getLong("customer_id"));
                    c.setTenantId(rs.getLong("tenant_id"));
                    c.setFirstName(rs.getString("first_name"));
                    c.setLastName(rs.getString("last_name"));
                    c.setPhone(rs.getString("phone"));
                    c.setEmail(rs.getString("email"));

                    long uid = rs.getLong("user_id");
                    c.setUserId(rs.wasNull() ? null : uid);

                    var ts = rs.getTimestamp("created_at");
                    c.setCreatedAt(ts == null ? null : ts.toLocalDateTime());

                    out.add(c);
                }
                return out;
            }
        }
    }

    public long insert(long tenantId, Customer c) throws Exception {
        String sql = """
                INSERT INTO customers (tenant_id, first_name, last_name, phone, email, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, tenantId);
            ps.setString(2, c.getFirstName());
            ps.setString(3, c.getLastName());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getEmail());

            if (c.getUserId() == null) ps.setNull(6, java.sql.Types.BIGINT);
            else ps.setLong(6, c.getUserId());

            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new IllegalStateException("customer_id alınamadı.");
                return rs.getLong(1);
            }
        }
    }

    public void update(long tenantId, Customer c) throws Exception {
        String sql = """
                UPDATE customers
                SET first_name = ?, last_name = ?, phone = ?, email = ?, user_id = ?
                WHERE tenant_id = ? AND customer_id = ?
                """;

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getFirstName());
            ps.setString(2, c.getLastName());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getEmail());

            if (c.getUserId() == null) ps.setNull(5, java.sql.Types.BIGINT);
            else ps.setLong(5, c.getUserId());

            ps.setLong(6, tenantId);
            ps.setLong(7, c.getCustomerId());

            ps.executeUpdate();
        }
    }

    public void delete(long tenantId, long customerId) throws Exception {
        String sql = "DELETE FROM customers WHERE tenant_id = ? AND customer_id = ?";

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, tenantId);
            ps.setLong(2, customerId);
            ps.executeUpdate();
        }
    }
}

package com.osman.vssfx.dao;

import com.osman.vssfx.model.Maintenance;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDAO {

    public List<Maintenance> findByVehicle(long tenantId, long vehicleId) throws Exception {
        String sql = """
                SELECT maint_id, tenant_id, vehicle_id, maint_date, maint_type, odometer_km, description, cost
                FROM maintenance
                WHERE tenant_id = ? AND vehicle_id = ?
                ORDER BY maint_date DESC, maint_id DESC
                """;

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, tenantId);
            ps.setLong(2, vehicleId);

            try (var rs = ps.executeQuery()) {
                List<Maintenance> out = new ArrayList<>();
                while (rs.next()) {
                    Maintenance m = new Maintenance();
                    m.setMaintId(rs.getLong("maint_id"));
                    m.setTenantId(rs.getLong("tenant_id"));
                    m.setVehicleId(rs.getLong("vehicle_id"));
                    m.setMaintDate(rs.getDate("maint_date").toLocalDate());
                    m.setMaintType(rs.getString("maint_type"));
                    m.setOdometerKm(rs.getInt("odometer_km"));
                    m.setDescription(rs.getString("description"));

                    BigDecimal cost = rs.getBigDecimal("cost");
                    m.setCost(cost == null ? BigDecimal.ZERO : cost);

                    out.add(m);
                }
                return out;
            }
        }
    }

    public long insert(long tenantId, Maintenance m) throws Exception {
        String sql = """
                INSERT INTO maintenance (tenant_id, maint_date, maint_type, odometer_km, description, cost, vehicle_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, tenantId);
            ps.setDate(2, java.sql.Date.valueOf(m.getMaintDate()));
            ps.setString(3, m.getMaintType());
            ps.setInt(4, m.getOdometerKm());
            ps.setString(5, m.getDescription());
            ps.setBigDecimal(6, m.getCost() == null ? BigDecimal.ZERO : m.getCost());
            ps.setLong(7, m.getVehicleId());

            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new IllegalStateException("maint_id alınamadı.");
                return rs.getLong(1);
            }
        }
    }

    public void update(long tenantId, Maintenance m) throws Exception {
        String sql = """
                UPDATE maintenance
                SET maint_date = ?, maint_type = ?, odometer_km = ?, description = ?, cost = ?
                WHERE tenant_id = ? AND maint_id = ?
                """;

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(m.getMaintDate()));
            ps.setString(2, m.getMaintType());
            ps.setInt(3, m.getOdometerKm());
            ps.setString(4, m.getDescription());
            ps.setBigDecimal(5, m.getCost() == null ? BigDecimal.ZERO : m.getCost());
            ps.setLong(6, tenantId);
            ps.setLong(7, m.getMaintId());

            ps.executeUpdate();
        }
    }

    public void delete(long tenantId, long maintId) throws Exception {
        String sql = "DELETE FROM maintenance WHERE tenant_id = ? AND maint_id = ?";

        try (var conn = com.osman.vssfx.db.Db.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setLong(1, tenantId);
            ps.setLong(2, maintId);
            ps.executeUpdate();
        }
    }
}

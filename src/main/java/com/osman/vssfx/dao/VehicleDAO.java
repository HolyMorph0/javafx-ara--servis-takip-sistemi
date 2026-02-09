package com.osman.vssfx.dao;

import com.osman.vssfx.db.Db;
import com.osman.vssfx.model.Vehicle;
import com.osman.vssfx.model.VehicleStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VehicleDAO {

    public List<Vehicle> findAllByTenant(long tenantId) throws SQLException {
        String sql = """
            SELECT vehicle_id, tenant_id, customer_id, public_id,
                   plate_no, vin_no, make, model, model_year, colour,
                   current_km, status, notes, service_entry_date, created_at
            FROM vehicle
            WHERE tenant_id = ?
            ORDER BY vehicle_id DESC
            """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, tenantId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Vehicle> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public long insert(long tenantId, Vehicle v) throws SQLException {
        String sql = """
            INSERT INTO vehicle
              (tenant_id, customer_id, public_id, plate_no, vin_no, make, model, model_year, colour,
               current_km, status, notes, service_entry_date)
            VALUES
              (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        if (v.getPublicId() == null || v.getPublicId().isBlank()) {
            v.setPublicId(UUID.randomUUID().toString());
        }

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, tenantId);

            if (v.getCustomerId() == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, v.getCustomerId());

            ps.setString(3, v.getPublicId());

            ps.setString(4, v.getPlateNo());

            if (v.getVinNo() == null || v.getVinNo().isBlank()) ps.setNull(5, Types.VARCHAR);
            else ps.setString(5, v.getVinNo());

            ps.setString(6, v.getMake());
            ps.setString(7, v.getModel());
            ps.setInt(8, v.getModelYear());

            if (v.getColour() == null || v.getColour().isBlank()) ps.setNull(9, Types.VARCHAR);
            else ps.setString(9, v.getColour());

            ps.setLong(10, v.getCurrentKm());

            VehicleStatus st = (v.getStatus() == null) ? VehicleStatus.ACTIVE : v.getStatus();
            ps.setString(11, st.name());

            if (v.getNotes() == null || v.getNotes().isBlank()) ps.setNull(12, Types.VARCHAR);
            else ps.setString(12, v.getNotes());

            if (v.getServiceEntryDate() == null) ps.setNull(13, Types.DATE);
            else ps.setDate(13, Date.valueOf(v.getServiceEntryDate()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                return -1;
            }
        }
    }

    public void update(long tenantId, Vehicle v) throws SQLException {
        String sql = """
            UPDATE vehicle
            SET plate_no=?, vin_no=?, make=?, model=?, model_year=?, colour=?,
                current_km=?, status=?, notes=?, service_entry_date=?
            WHERE vehicle_id=? AND tenant_id=?
            """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, v.getPlateNo());

            if (v.getVinNo() == null || v.getVinNo().isBlank()) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, v.getVinNo());

            ps.setString(3, v.getMake());
            ps.setString(4, v.getModel());
            ps.setInt(5, v.getModelYear());

            if (v.getColour() == null || v.getColour().isBlank()) ps.setNull(6, Types.VARCHAR);
            else ps.setString(6, v.getColour());

            ps.setLong(7, v.getCurrentKm());

            VehicleStatus st = (v.getStatus() == null) ? VehicleStatus.ACTIVE : v.getStatus();
            ps.setString(8, st.name());

            if (v.getNotes() == null || v.getNotes().isBlank()) ps.setNull(9, Types.VARCHAR);
            else ps.setString(9, v.getNotes());

            if (v.getServiceEntryDate() == null) ps.setNull(10, Types.DATE);
            else ps.setDate(10, Date.valueOf(v.getServiceEntryDate()));

            ps.setLong(11, v.getVehicleId());
            ps.setLong(12, tenantId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Güncelleme yapılamadı (kayıt bulunamadı veya tenant uyuşmuyor).");
            }
        }
    }

    public void delete(long tenantId, long vehicleId) throws SQLException {
        String sql = "DELETE FROM vehicle WHERE tenant_id=? AND vehicle_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, vehicleId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Silme yapılamadı (kayıt bulunamadı veya tenant uyuşmuyor).");
            }
        }
    }

    private Vehicle map(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setVehicleId(rs.getLong("vehicle_id"));
        v.setTenantId(rs.getLong("tenant_id"));

        long cust = rs.getLong("customer_id");
        v.setCustomerId(rs.wasNull() ? null : cust);

        v.setPublicId(rs.getString("public_id"));
        v.setPlateNo(rs.getString("plate_no"));
        v.setVinNo(rs.getString("vin_no"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setModelYear(rs.getInt("model_year"));
        v.setColour(rs.getString("colour"));
        v.setCurrentKm(rs.getLong("current_km"));

        v.setStatus(VehicleStatus.fromDb(rs.getString("status")));

        v.setNotes(rs.getString("notes"));

        Date d = rs.getDate("service_entry_date");
        v.setServiceEntryDate(d == null ? null : d.toLocalDate());

        Timestamp ts = rs.getTimestamp("created_at");
        v.setCreatedAt(ts == null ? null : ts.toLocalDateTime());

        return v;
    }
}

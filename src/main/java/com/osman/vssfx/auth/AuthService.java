package com.osman.vssfx.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.osman.vssfx.dao.UserDAO;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public void login(long tenantId, String email, String password) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("E-posta boş olamaz.");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Şifre boş olamaz.");

        var row = userDAO.findActiveByTenantAndEmail(tenantId, email.trim());
        if (row == null) throw new IllegalArgumentException("Kullanıcı bulunamadı.");

        if (!"ACTIVE".equalsIgnoreCase(row.status())) {
            throw new IllegalArgumentException("Kullanıcı devre dışı.");
        }

        String hash = row.passwordHash();
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Bu kullanıcı için şifre tanımlı değil.");
        }

        // Dev kolaylığı: Hash bcrypt değilse (ör. düz yazı) eşitlik kontrolü yap.
        boolean ok;
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            ok = BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
        } else {
            ok = password.equals(hash);
        }

        if (!ok) throw new IllegalArgumentException("Şifre hatalı.");

        userDAO.updateLastLogin(row.userId());
        SessionContext.set(row.tenantId(), row.userId(), row.role(), row.email());
    }

    // ✅ Tenant + Admin kullanıcı kaydı (SQL şemana uygun)
    public long registerTenantAndAdmin(String companyName, String fullName, String email, String rawPassword) throws Exception {
        if (companyName == null || companyName.isBlank()) throw new IllegalArgumentException("Firma adı boş olamaz.");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Ad soyad boş olamaz.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("E-posta boş olamaz.");
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("Şifre boş olamaz.");

        String normalizedEmail = email.trim().toLowerCase();

        // ✅ favre BCrypt ile hash üret (login verifyer ile uyumlu)
        String hash = BCrypt.withDefaults().hashToString(10, rawPassword.toCharArray());

        try (var conn = com.osman.vssfx.db.Db.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1) Tenant oluştur
                long tenantId;
                try (var ps = conn.prepareStatement(
                        "INSERT INTO tenants (name) VALUES (?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS
                )) {
                    ps.setString(1, companyName.trim());
                    ps.executeUpdate();

                    try (var rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new IllegalStateException("Tenant ID alınamadı.");
                        tenantId = rs.getLong(1);
                    }
                }

                // 2) Aynı tenant içinde email var mı kontrol
                try (var ps = conn.prepareStatement(
                        "SELECT 1 FROM users WHERE tenant_id = ? AND email = ? LIMIT 1"
                )) {
                    ps.setLong(1, tenantId);
                    ps.setString(2, normalizedEmail);
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) throw new IllegalStateException("Bu e-posta zaten kayıtlı.");
                    }
                }

                // 3) Admin kullanıcı oluştur (users tablosunda full_name yok)
                // users enumlarına göre:
                // role: SERVICE_ADMIN / SERVICE_STAFF / CUSTOMER
                // status: ACTIVE / DISABLED
                try (var ps = conn.prepareStatement(
                        "INSERT INTO users (tenant_id, role, status, email, password_hash) VALUES (?, ?, ?, ?, ?)"
                )) {
                    ps.setLong(1, tenantId);
                    ps.setString(2, "SERVICE_ADMIN");
                    ps.setString(3, "ACTIVE");
                    ps.setString(4, normalizedEmail);
                    ps.setString(5, hash);
                    ps.executeUpdate();
                }

                // Not: fullName şu an DB'de saklanacak kolon olmadığı için kullanılmıyor.
                // İstersen users tablosuna full_name ekleyip burada kaydederiz.

                conn.commit();
                return tenantId;

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}

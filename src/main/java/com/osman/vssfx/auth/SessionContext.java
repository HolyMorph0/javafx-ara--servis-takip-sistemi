package com.osman.vssfx.auth;

public final class SessionContext {
    private static Long tenantId;
    private static Long userId;
    private static String role;
    private static String email;

    private SessionContext() {}

    public static void set(Long tenantId, Long userId, String role, String email) {
        SessionContext.tenantId = tenantId;
        SessionContext.userId = userId;
        SessionContext.role = role;
        SessionContext.email = email;
    }

    public static void clear() {
        tenantId = null;
        userId = null;
        role = null;
        email = null;
    }

    public static Long tenantId() { return tenantId; }
    public static Long userId() { return userId; }
    public static String role() { return role; }
    public static String email() { return email; }

    public static boolean isLoggedIn() {
        return tenantId != null && userId != null;
    }
}

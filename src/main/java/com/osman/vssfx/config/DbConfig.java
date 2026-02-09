package com.osman.vssfx.config;

import java.io.InputStream;
import java.util.Properties;

public final class DbConfig {
    private static final String PROPS_FILE = "/db.properties";
    private static final Properties PROPS = load();

    private DbConfig() {}

    private static Properties load() {
        try (InputStream in = DbConfig.class.getResourceAsStream(PROPS_FILE)) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on classpath: " + PROPS_FILE);
            }
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    public static String url() { return required("db.url"); }
    public static String user() { return required("db.user"); }
    public static String password() { return required("db.password"); }
    public static String driver() { return PROPS.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"); }

    private static String required(String key) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing property: " + key);
        return v.trim();
    }
}

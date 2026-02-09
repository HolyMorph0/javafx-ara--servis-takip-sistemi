package com.osman.vssfx.ui;

import javafx.scene.Scene;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class ThemeManager {

    public enum Theme { LIGHT, DARK }

    // Theme CSS resources (classpath)
    private static final String LIGHT_CSS = "/com/osman/vssfx/ui/theme-light.css";
    private static final String DARK_CSS  = "/com/osman/vssfx/ui/theme-dark.css";

    // Preference storage (user home)
    private static final String APP_DIR_NAME = ".vssfx";
    private static final String PREF_FILE_NAME = "theme.properties";
    private static final String KEY = "theme";

    private ThemeManager() {}

    private static Path prefPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, APP_DIR_NAME, PREF_FILE_NAME);
    }

    public static Theme loadTheme() {
        Path path = prefPath();
        if (!Files.exists(path)) return Theme.LIGHT;

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            String raw = p.getProperty(KEY, Theme.LIGHT.name());
            return Theme.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Theme.LIGHT;
        }
    }

    public static void saveTheme(Theme theme) {
        try {
            Path path = prefPath();
            Files.createDirectories(path.getParent());

            Properties p = new Properties();
            p.setProperty(KEY, theme.name());

            try (OutputStream out = Files.newOutputStream(path)) {
                p.store(out, "VSSFX UI Theme");
            }
        } catch (Exception ignored) {}
    }

    /** Convenience: loads saved theme and applies it. */
    public static void apply(Scene scene) {
        apply(scene, loadTheme());
    }

    /** Applies given theme to the scene without removing non-theme stylesheets. */
    public static void apply(Scene scene, Theme theme) {
        if (scene == null) return;

        // Remove only our theme css (do NOT clear everything)
        scene.getStylesheets().removeIf(s ->
                s != null && (s.endsWith("theme-light.css") || s.endsWith("theme-dark.css"))
        );

        String resourcePath = (theme == Theme.DARK) ? DARK_CSS : LIGHT_CSS;
        var url = ThemeManager.class.getResource(resourcePath);

        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }

        // Persist choice
        saveTheme(theme);
    }

    /** Toggles theme, applies, and persists. Returns the new theme. */
    public static Theme toggle(Scene scene) {
        Theme next = (loadTheme() == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        apply(scene, next);
        return next;
    }
}

package com.osman.vssfx.ui.controllers;

import com.osman.vssfx.auth.SessionContext;
import com.osman.vssfx.ui.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;

public class MainController {

    @FXML private ComboBox<String> themeBox;

    @FXML
    public void initialize() {
        // Login kontrolü: oturum yoksa LoginView'e dön
        if (!SessionContext.isLoggedIn()) {
            goToLogin();
            return;
        }

        // Tema seçenekleri Türkçe görünsün
        themeBox.getItems().setAll("Açık", "Koyu");

        var current = ThemeManager.loadTheme();
        themeBox.getSelectionModel().select(current == ThemeManager.Theme.DARK ? "Koyu" : "Açık");

        themeBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            ThemeManager.Theme t = "Koyu".equals(newV) ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT;

            var scene = themeBox.getScene();
            if (scene == null) return;

            ThemeManager.apply(scene, t);
            ThemeManager.saveTheme(t);
        });
    }

    @FXML
    public void onLogout() {
        SessionContext.clear();
        goToLogin();
    }

    private void goToLogin() {
        try {
            var scene = (themeBox != null) ? themeBox.getScene() : null;
            if (scene == null) return;

            var url = getClass().getResource("/com/osman/vssfx/ui/views/LoginView.fxml");
            if (url == null) throw new IllegalStateException("LoginView.fxml bulunamadı!");

            Parent loginRoot = FXMLLoader.load(url);
            scene.setRoot(loginRoot);

            // tema login ekranına da uygulansın
            ThemeManager.apply(scene, ThemeManager.loadTheme());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

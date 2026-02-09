package com.osman.vssfx.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        var url = getClass().getResource("/com/osman/vssfx/ui/views/LoginView.fxml");
        if (url == null) throw new IllegalStateException("LoginView.fxml bulunamadı!");

        var loader = new FXMLLoader(url);
        Parent root = (Parent) loader.load();   // ✅ cast

        var scene = new Scene(root, 1100, 700);

        var theme = com.osman.vssfx.ui.ThemeManager.loadTheme();
        com.osman.vssfx.ui.ThemeManager.apply(scene, theme);


        System.out.println("DB Ping: " + com.osman.vssfx.db.Db.ping());

        stage.setTitle("Araç Takip Sistemi");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}

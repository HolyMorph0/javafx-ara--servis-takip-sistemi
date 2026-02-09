package com.osman.vssfx.ui.controllers;

import com.osman.vssfx.auth.AuthService;
import com.osman.vssfx.ui.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField tenantIdField;
    @FXML private TextField emailField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;

    @FXML private Button loginBtn;
    @FXML private ProgressIndicator loginSpinner;
    @FXML private Label loginBtnText;

    @FXML private Label msgLabel;

    private final AuthService auth = new AuthService();

    @FXML
    public void initialize() {
        tenantIdField.setText("1"); // istersen sonra kaldırırız
        emailField.requestFocus();

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        passwordField.setOnAction(e -> onLogin());
        passwordVisibleField.setOnAction(e -> onLogin());
    }

    @FXML
    public void onTogglePassword() {
        boolean show = showPasswordBtn.isSelected();

        passwordVisibleField.setVisible(show);
        passwordVisibleField.setManaged(show);

        passwordField.setVisible(!show);
        passwordField.setManaged(!show);

        showPasswordBtn.setText(show ? "🙈" : "👁");

        Platform.runLater(() -> {
            if (show) passwordVisibleField.requestFocus();
            else passwordField.requestFocus();
        });
    }

    @FXML
    public void onGoRegister() {
        try {
            var url = getClass().getResource("/com/osman/vssfx/ui/views/RegisterView.fxml");
            if (url == null) throw new IllegalStateException("RegisterView.fxml bulunamadı!");

            Parent registerRoot = FXMLLoader.load(url);

            var scene = loginBtn.getScene();
            scene.setRoot(registerRoot);

            ThemeManager.apply(scene, ThemeManager.loadTheme());
        } catch (Exception e) {
            e.printStackTrace();
            msgLabel.setText("Kayıt ekranı açılamadı: " + e.getMessage());
        }
    }

    @FXML
    public void onLogin() {
        msgLabel.setText("");

        long tenantId;
        try {
            tenantId = Long.parseLong(tenantIdField.getText().trim());
            if (tenantId <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            msgLabel.setText("Tenant ID geçersiz.");
            return;
        }

        String email = emailField.getText();
        String pass = passwordField.getText();

        setLoading(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                auth.login(tenantId, email, pass);
                return null;
            }
        };

        task.setOnSucceeded(ev -> {
            try {
                var url = getClass().getResource("/com/osman/vssfx/ui/views/MainView.fxml");
                if (url == null) throw new IllegalStateException("MainView.fxml bulunamadı!");
                Parent mainRoot = FXMLLoader.load(url);

                var scene = loginBtn.getScene();
                scene.setRoot(mainRoot);

                ThemeManager.apply(scene, ThemeManager.loadTheme());

            } catch (Exception ex) {
                ex.printStackTrace();
                msgLabel.setText("Ana ekran yüklenemedi: " + ex.getMessage());
                setLoading(false);
            }
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            String m = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Giriş başarısız.";
            msgLabel.setText(m);
            setLoading(false);
        });

        new Thread(task, "login-task").start();
    }

    private void setLoading(boolean loading) {
        loginBtn.setDisable(loading);

        loginSpinner.setVisible(loading);
        loginSpinner.setManaged(loading);

        loginBtnText.setText(loading ? "Giriş yapılıyor..." : "Giriş");
    }
}

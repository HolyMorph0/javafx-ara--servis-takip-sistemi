package com.osman.vssfx.ui.controllers;

import com.osman.vssfx.auth.AuthService;
import com.osman.vssfx.ui.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField companyField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;

    @FXML private PasswordField password2Field;

    @FXML private Button registerBtn;
    @FXML private ProgressIndicator registerSpinner;
    @FXML private Label registerBtnText;

    @FXML private Label msgLabel;
    @FXML private Label okLabel;

    private final AuthService auth = new AuthService();

    @FXML
    public void initialize() {
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        passwordField.setOnAction(e -> onRegister());
        passwordVisibleField.setOnAction(e -> onRegister());
        password2Field.setOnAction(e -> onRegister());
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
    public void onBackToLogin() {
        try {
            var url = getClass().getResource("/com/osman/vssfx/ui/views/LoginView.fxml");
            if (url == null) throw new IllegalStateException("LoginView.fxml bulunamadı!");

            Parent loginRoot = FXMLLoader.load(url);

            var scene = registerBtn.getScene();
            scene.setRoot(loginRoot);

            ThemeManager.apply(scene, ThemeManager.loadTheme());
        } catch (Exception e) {
            e.printStackTrace();
            msg("Giriş ekranına dönülemedi: " + e.getMessage());
        }
    }

    @FXML
    public void onRegister() {
        clearMessages();

        String company = text(companyField);
        String fullName = text(fullNameField);
        String email = text(emailField);
        String pass1 = passwordField.getText() == null ? "" : passwordField.getText();
        String pass2 = password2Field.getText() == null ? "" : password2Field.getText();

        if (company.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
            msg("Firma adı, ad soyad ve e-posta zorunludur.");
            return;
        }
        if (pass1.length() < 6) {
            msg("Şifre en az 6 karakter olmalıdır.");
            return;
        }
        if (!pass1.equals(pass2)) {
            msg("Şifreler uyuşmuyor.");
            return;
        }

        setLoading(true);

        Task<Long> task = new Task<>() {
            @Override protected Long call() throws Exception {
                return auth.registerTenantAndAdmin(company, fullName, email, pass1);
            }
        };

        task.setOnSucceeded(ev -> {
            Long tenantId = task.getValue();
            ok("Kayıt başarılı ✅ Tenant ID: " + tenantId + "  (Giriş ekranında Tenant ID alanına bunu yaz)");
            setLoading(false);
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            String m = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Kayıt başarısız.";
            msg(m);
            setLoading(false);
        });

        new Thread(task, "register-task").start();
    }

    private String text(TextField f) {
        return (f.getText() == null) ? "" : f.getText().trim();
    }

    private void clearMessages() {
        msgLabel.setText("");
        okLabel.setText("");
    }

    private void msg(String s) { msgLabel.setText(s == null ? "" : s); }
    private void ok(String s) { okLabel.setText(s == null ? "" : s); }

    private void setLoading(boolean loading) {
        registerBtn.setDisable(loading);

        registerSpinner.setVisible(loading);
        registerSpinner.setManaged(loading);

        registerBtnText.setText(loading ? "Kayıt yapılıyor..." : "Kayıt Ol");
    }
}

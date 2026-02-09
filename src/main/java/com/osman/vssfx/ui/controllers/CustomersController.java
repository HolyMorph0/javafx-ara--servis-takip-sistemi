package com.osman.vssfx.ui.controllers;

import com.osman.vssfx.auth.SessionContext;
import com.osman.vssfx.dao.CustomerDAO;
import com.osman.vssfx.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;

public class CustomersController {

    private final CustomerDAO dao = new CustomerDAO();

    private final ObservableList<Customer> master = FXCollections.observableArrayList();
    private FilteredList<Customer> filtered;

    @FXML private TextField searchField;

    @FXML private TableView<Customer> table;
    @FXML private TableColumn<Customer, Long> colId;
    @FXML private TableColumn<Customer, String> colFirstName;
    @FXML private TableColumn<Customer, String> colLastName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colCreatedAt;

    @FXML private TextField firstNameField, lastNameField, phoneField, emailField;
    @FXML private Label msgLabel;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colCreatedAt.setCellValueFactory(cell -> {
            var v = cell.getValue();
            String t = (v.getCreatedAt() == null) ? "" : DT.format(v.getCreatedAt());
            return new javafx.beans.property.SimpleStringProperty(t);
        });

        filtered = new FilteredList<>(master, x -> true);
        table.setItems(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        searchField.textProperty().addListener((obs, oldV, q) -> applyFilter(q));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, c) -> {
            if (c != null) fillForm(c);
        });

        reload();
    }

    private long tenantId() {
        Long tid = SessionContext.tenantId();
        if (tid == null) throw new IllegalStateException("Oturum bulunamadı. Lütfen tekrar giriş yapın.");
        return tid;
    }

    private void applyFilter(String q) {
        String s = (q == null) ? "" : q.trim().toLowerCase();
        filtered.setPredicate(c -> {
            if (s.isEmpty()) return true;
            return safe(c.getFirstName()).contains(s)
                    || safe(c.getLastName()).contains(s)
                    || safe(c.getPhone()).contains(s)
                    || safe(c.getEmail()).contains(s);
        });
    }

    private String safe(String x) { return x == null ? "" : x.toLowerCase(); }

    private void reload() {
        try {
            master.setAll(dao.findAllByTenant(tenantId()));
            msg("Yüklendi: " + master.size());
        } catch (Exception e) {
            e.printStackTrace();
            msg("Veritabanı hatası: " + e.getMessage());
        }
    }

    @FXML public void onRefresh() { reload(); }

    @FXML
    public void onAdd() {
        try {
            Customer c = readFormForInsert();
            long id = dao.insert(tenantId(), c);
            msg("Eklendi. ID=" + id);
            reload();
            onClear();
        } catch (Exception e) {
            e.printStackTrace();
            msg("Ekleme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onUpdate() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msg("Önce bir satır seçin."); return; }

        try {
            Customer c = readFormForUpdate(selected);
            dao.update(tenantId(), c);
            msg("Güncellendi.");
            reload();
        } catch (Exception e) {
            e.printStackTrace();
            msg("Güncelleme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msg("Önce bir satır seçin."); return; }

        try {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Silme Onayı");
            a.setHeaderText("Seçili müşteri silinsin mi?");
            a.setContentText("Bu işlem geri alınamaz.");

            ButtonType ok = new ButtonType("Evet", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Vazgeç", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(ok, cancel);

            if (a.showAndWait().orElse(cancel) != ok) return;

            dao.delete(tenantId(), selected.getCustomerId());
            msg("Silindi.");
            reload();
            onClear();
        } catch (Exception e) {
            e.printStackTrace();
            msg("Silme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onClear() {
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        emailField.clear();
        table.getSelectionModel().clearSelection();
        msg("");
    }

    private Customer readFormForInsert() {
        Customer c = new Customer();
        c.setTenantId(tenantId());
        applyFormTo(c);
        return c;
    }

    private Customer readFormForUpdate(Customer base) {
        Customer c = new Customer();
        c.setCustomerId(base.getCustomerId());
        c.setTenantId(tenantId());
        c.setUserId(base.getUserId()); // varsa kalsın
        c.setCreatedAt(base.getCreatedAt()); // UI'da göstermelik, DB'de değişmiyor
        applyFormTo(c);
        return c;
    }

    private void applyFormTo(Customer c) {
        String fn = text(firstNameField);
        String ln = text(lastNameField);

        if (fn.isEmpty() || ln.isEmpty()) {
            throw new IllegalArgumentException("Ad ve Soyad boş olamaz.");
        }

        c.setFirstName(fn);
        c.setLastName(ln);
        c.setPhone(blankToNull(text(phoneField)));
        c.setEmail(blankToNull(text(emailField)));
    }

    private String text(TextField f) { return (f.getText() == null) ? "" : f.getText().trim(); }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void fillForm(Customer c) {
        firstNameField.setText(c.getFirstName() == null ? "" : c.getFirstName());
        lastNameField.setText(c.getLastName() == null ? "" : c.getLastName());
        phoneField.setText(c.getPhone() == null ? "" : c.getPhone());
        emailField.setText(c.getEmail() == null ? "" : c.getEmail());
    }

    private void msg(String s) { msgLabel.setText(s == null ? "" : s); }
}

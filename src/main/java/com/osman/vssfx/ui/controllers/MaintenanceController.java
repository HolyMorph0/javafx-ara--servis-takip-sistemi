package com.osman.vssfx.ui.controllers;

import com.osman.vssfx.auth.SessionContext;
import com.osman.vssfx.dao.VehicleDAO;
import com.osman.vssfx.model.Vehicle;
import com.osman.vssfx.dao.MaintenanceDAO;
import com.osman.vssfx.model.Maintenance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MaintenanceController {

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();

    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private final ObservableList<Maintenance> master = FXCollections.observableArrayList();

    @FXML private ComboBox<Vehicle> vehicleBox;

    @FXML private TableView<Maintenance> table;
    @FXML private TableColumn<Maintenance, Long> colId;
    @FXML private TableColumn<Maintenance, LocalDate> colDate;
    @FXML private TableColumn<Maintenance, String> colType;
    @FXML private TableColumn<Maintenance, Integer> colKm;
    @FXML private TableColumn<Maintenance, BigDecimal> colCost;
    @FXML private TableColumn<Maintenance, String> colDesc;

    @FXML private DatePicker datePicker;
    @FXML private TextField typeField;
    @FXML private TextField kmField;
    @FXML private TextField costField;
    @FXML private TextArea descArea;
    @FXML private Label msgLabel;

    @FXML
    public void initialize() {
        // araç combo
        vehicleBox.setItems(vehicles);
        vehicleBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Vehicle v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatVehicle(v));
            }
        });
        vehicleBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Vehicle v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatVehicle(v));
            }
        });

        vehicleBox.valueProperty().addListener((obs, oldV, v) -> {
            onClear();
            if (v != null) reloadMaintenance(v.getVehicleId());
            else master.clear();
        });

        // tablo bağları
        colId.setCellValueFactory(new PropertyValueFactory<>("maintId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("maintDate"));
        colType.setCellValueFactory(new PropertyValueFactory<>("maintType"));
        colKm.setCellValueFactory(new PropertyValueFactory<>("odometerKm"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.setItems(master);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, m) -> {
            if (m != null) fillForm(m);
        });

        // sayısal formatlar
        kmField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,10}") ? c : null
        ));

        // 0-2 ondalık destek (nokta ile)
        costField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,10}(\\.\\d{0,2})?") ? c : null
        ));

        reloadVehicles();
    }

    private long tenantId() {
        Long tid = SessionContext.tenantId();
        if (tid == null) throw new IllegalStateException("Oturum bulunamadı. Lütfen tekrar giriş yapın.");
        return tid;
    }

    private String formatVehicle(Vehicle v) {
        String plate = v.getPlateNo() == null ? "" : v.getPlateNo();
        String make  = v.getMake() == null ? "" : v.getMake();
        String model = v.getModel() == null ? "" : v.getModel();
        return plate + " • " + make + " " + model;
    }

    private void reloadVehicles() {
        try {
            vehicles.setAll(vehicleDAO.findAllByTenant(tenantId()));
            if (!vehicles.isEmpty()) vehicleBox.getSelectionModel().selectFirst();
            msg("Araçlar yüklendi: " + vehicles.size());
        } catch (Exception e) {
            e.printStackTrace();
            msg("Araçlar yüklenemedi: " + e.getMessage());
        }
    }

    private void reloadMaintenance(long vehicleId) {
        try {
            master.setAll(maintenanceDAO.findByVehicle(tenantId(), vehicleId));
            msg("Bakım kayıtları yüklendi: " + master.size());
        } catch (Exception e) {
            e.printStackTrace();
            msg("Bakım kayıtları yüklenemedi: " + e.getMessage());
        }
    }

    @FXML
    public void onRefresh() {
        reloadVehicles();
        Vehicle v = vehicleBox.getValue();
        if (v != null) reloadMaintenance(v.getVehicleId());
    }

    @FXML
    public void onAdd() {
        Vehicle v = vehicleBox.getValue();
        if (v == null) { msg("Önce araç seçin."); return; }

        try {
            Maintenance m = readFormForInsert(v.getVehicleId());
            long id = maintenanceDAO.insert(tenantId(), m);
            msg("Eklendi. ID=" + id);
            reloadMaintenance(v.getVehicleId());
            onClear();
        } catch (Exception e) {
            e.printStackTrace();
            msg("Ekleme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onUpdate() {
        Vehicle v = vehicleBox.getValue();
        if (v == null) { msg("Önce araç seçin."); return; }

        Maintenance selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msg("Önce bir kayıt seçin."); return; }

        try {
            Maintenance m = readFormForUpdate(selected, v.getVehicleId());
            maintenanceDAO.update(tenantId(), m);
            msg("Güncellendi.");
            reloadMaintenance(v.getVehicleId());
        } catch (Exception e) {
            e.printStackTrace();
            msg("Güncelleme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        Vehicle v = vehicleBox.getValue();
        if (v == null) { msg("Önce araç seçin."); return; }

        Maintenance selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msg("Önce bir kayıt seçin."); return; }

        try {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Silme Onayı");
            a.setHeaderText("Seçili bakım kaydı silinsin mi?");
            a.setContentText("Bu işlem geri alınamaz.");

            ButtonType ok = new ButtonType("Evet", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Vazgeç", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(ok, cancel);

            if (a.showAndWait().orElse(cancel) != ok) return;

            maintenanceDAO.delete(tenantId(), selected.getMaintId());
            msg("Silindi.");
            reloadMaintenance(v.getVehicleId());
            onClear();
        } catch (Exception e) {
            e.printStackTrace();
            msg("Silme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onClear() {
        datePicker.setValue(null);
        typeField.clear();
        kmField.clear();
        costField.clear();
        descArea.clear();
        table.getSelectionModel().clearSelection();
        msg("");
    }

    private Maintenance readFormForInsert(long vehicleId) {
        Maintenance m = new Maintenance();
        m.setTenantId(tenantId());
        m.setVehicleId(vehicleId);
        applyFormTo(m);
        return m;
    }

    private Maintenance readFormForUpdate(Maintenance base, long vehicleId) {
        Maintenance m = new Maintenance();
        m.setMaintId(base.getMaintId());
        m.setTenantId(tenantId());
        m.setVehicleId(vehicleId);
        applyFormTo(m);
        return m;
    }

    private void applyFormTo(Maintenance m) {
        LocalDate date = datePicker.getValue();
        String type = typeField.getText() == null ? "" : typeField.getText().trim();
        String kmRaw = kmField.getText() == null ? "" : kmField.getText().trim();
        String costRaw = costField.getText() == null ? "" : costField.getText().trim();

        if (date == null) throw new IllegalArgumentException("Tarih boş olamaz.");
        if (type.isEmpty()) throw new IllegalArgumentException("Bakım türü boş olamaz.");
        if (kmRaw.isEmpty()) throw new IllegalArgumentException("KM boş olamaz.");

        int km = Integer.parseInt(kmRaw);

        BigDecimal cost = BigDecimal.ZERO;
        if (!costRaw.isEmpty()) cost = new BigDecimal(costRaw);

        m.setMaintDate(date);
        m.setMaintType(type);
        m.setOdometerKm(km);
        m.setCost(cost);
        m.setDescription(blankToNull(descArea.getText()));
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void fillForm(Maintenance m) {
        datePicker.setValue(m.getMaintDate());
        typeField.setText(m.getMaintType() == null ? "" : m.getMaintType());
        kmField.setText(String.valueOf(m.getOdometerKm()));
        costField.setText(m.getCost() == null ? "" : m.getCost().toPlainString());
        descArea.setText(m.getDescription() == null ? "" : m.getDescription());
    }

    private void msg(String s) { msgLabel.setText(s == null ? "" : s); }
}

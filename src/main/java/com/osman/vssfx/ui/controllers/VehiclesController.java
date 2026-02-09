package com.osman.vssfx.ui.controllers;

import com.osman.vssfx.auth.SessionContext;
import com.osman.vssfx.dao.VehicleDAO;
import com.osman.vssfx.model.Vehicle;
import com.osman.vssfx.model.VehicleStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class VehiclesController {

    private final VehicleDAO dao = new VehicleDAO();
    private final ObservableList<Vehicle> master = FXCollections.observableArrayList();
    private FilteredList<Vehicle> filtered;

    @FXML private TextField searchField;

    @FXML private TableView<Vehicle> table;
    @FXML private TableColumn<Vehicle, Long> colId;
    @FXML private TableColumn<Vehicle, String> colPlate;
    @FXML private TableColumn<Vehicle, String> colMake;
    @FXML private TableColumn<Vehicle, String> colModel;
    @FXML private TableColumn<Vehicle, Integer> colYear;
    @FXML private TableColumn<Vehicle, Long> colKm;
    @FXML private TableColumn<Vehicle, String> colStatus;

    @FXML private TextField plateField, makeField, modelField, yearField, kmField, colorField;
    @FXML private ComboBox<VehicleStatus> statusBox;
    @FXML private DatePicker serviceDatePicker;
    @FXML private TextArea notesArea;
    @FXML private Label msgLabel;

    @FXML
    public void initialize() {
        // Tablo kolon bağları
        colId.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colPlate.setCellValueFactory(new PropertyValueFactory<>("plateNo"));
        colMake.setCellValueFactory(new PropertyValueFactory<>("make"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("modelYear"));
        colKm.setCellValueFactory(new PropertyValueFactory<>("currentKm"));

        // Durum metni (Türkçe)
        colStatus.setCellValueFactory(cell -> {
            VehicleStatus st = cell.getValue().getStatus();
            String text = statusText(st);
            return new SimpleStringProperty(text);
        });

        // Durum rozeti (CSS class'ı status'a göre)
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().removeAll("badge-active", "badge-inservice", "badge-assigned", "badge-inactive");

                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);
                setGraphic(null);

                // CSS class mapping: metin Türkçe olsa da status'tan türetiyoruz
                Vehicle v = getTableRow() == null ? null : (Vehicle) getTableRow().getItem();
                VehicleStatus st = (v == null) ? null : v.getStatus();

                switch (st) {
                    case ACTIVE -> getStyleClass().add("badge-active");
                    case IN_SERVICE -> getStyleClass().add("badge-inservice");
                    case ASSIGNED -> getStyleClass().add("badge-assigned");
                    case INACTIVE -> getStyleClass().add("badge-inactive");
                    default -> { /* noop */ }
                }
            }
        });
        colStatus.setMinWidth(120);
        colStatus.setMaxWidth(140);

        // Durum combobox
        statusBox.setItems(FXCollections.observableArrayList(VehicleStatus.values()));
        statusBox.setValue(VehicleStatus.ACTIVE);

        // ComboBox'ta enum yerine Türkçe göster
        statusBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(VehicleStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : statusText(item));
            }
        });
        statusBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VehicleStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : statusText(item));
            }
        });

        // Sadece sayısal giriş
        yearField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,4}") ? c : null));

        kmField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,10}") ? c : null));

        filtered = new FilteredList<>(master, v -> true);
        table.setItems(filtered);

        // Kolonlar boşluğu doldursun
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        searchField.textProperty().addListener((obs, oldV, q) -> applyFilter(q));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v != null) fillForm(v);
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
        filtered.setPredicate(v -> {
            if (s.isEmpty()) return true;
            return safe(v.getPlateNo()).contains(s)
                    || safe(v.getMake()).contains(s)
                    || safe(v.getModel()).contains(s);
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
            Vehicle v = readFormForInsert();
            long id = dao.insert(tenantId(), v);
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
        Vehicle selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msg("Önce bir satır seçin."); return; }

        try {
            Vehicle v = readFormForUpdate(selected);
            dao.update(tenantId(), v);
            msg("Güncellendi.");
            reload();
        } catch (Exception e) {
            e.printStackTrace();
            msg("Güncelleme başarısız: " + e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        Vehicle selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { msg("Önce bir satır seçin."); return; }

        try {
            // Basit onay (Türkçe)
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Silme Onayı");
            a.setHeaderText("Seçili araç silinsin mi?");
            a.setContentText("Bu işlem geri alınamaz.");

            ButtonType ok = new ButtonType("Evet", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Vazgeç", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(ok, cancel);

            if (a.showAndWait().orElse(cancel) != ok) return;

            dao.delete(tenantId(), selected.getVehicleId());
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
        plateField.clear();
        makeField.clear();
        modelField.clear();
        yearField.clear();
        kmField.clear();
        colorField.clear();
        notesArea.clear();
        serviceDatePicker.setValue(null);
        statusBox.setValue(VehicleStatus.ACTIVE);
        table.getSelectionModel().clearSelection();
        msg("");
    }

    private Vehicle readFormForInsert() {
        Vehicle v = new Vehicle();
        v.setTenantId(tenantId());
        applyFormToVehicle(v);
        return v;
    }

    private Vehicle readFormForUpdate(Vehicle base) {
        Vehicle v = new Vehicle();
        v.setVehicleId(base.getVehicleId());
        v.setTenantId(tenantId());
        v.setPublicId(base.getPublicId()); // değişmesin
        v.setCustomerId(base.getCustomerId());
        applyFormToVehicle(v);
        return v;
    }

    private void applyFormToVehicle(Vehicle v) {
        String plate = plateField.getText() == null ? "" : plateField.getText().trim();
        String make  = makeField.getText()  == null ? "" : makeField.getText().trim();
        String model = modelField.getText() == null ? "" : modelField.getText().trim();

        if (plate.isEmpty() || make.isEmpty() || model.isEmpty()) {
            throw new IllegalArgumentException("Plaka / Marka / Model boş olamaz.");
        }

        String yearRaw = yearField.getText() == null ? "" : yearField.getText().trim();
        String kmRaw   = kmField.getText() == null ? "" : kmField.getText().trim();
        if (yearRaw.isEmpty() || kmRaw.isEmpty()) {
            throw new IllegalArgumentException("Model yılı ve KM boş olamaz.");
        }

        int year = Integer.parseInt(yearRaw);
        long km  = Long.parseLong(kmRaw);

        v.setPlateNo(plate);
        v.setMake(make);
        v.setModel(model);
        v.setModelYear(year);
        v.setCurrentKm(km);

        v.setColour(blankToNull(colorField.getText()));
        v.setStatus(statusBox.getValue() == null ? VehicleStatus.ACTIVE : statusBox.getValue());
        v.setNotes(blankToNull(notesArea.getText()));
        v.setServiceEntryDate(serviceDatePicker.getValue());
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void fillForm(Vehicle v) {
        plateField.setText(v.getPlateNo());
        makeField.setText(v.getMake());
        modelField.setText(v.getModel());
        yearField.setText(String.valueOf(v.getModelYear()));
        kmField.setText(String.valueOf(v.getCurrentKm()));
        colorField.setText(v.getColour() == null ? "" : v.getColour());
        notesArea.setText(v.getNotes() == null ? "" : v.getNotes());
        serviceDatePicker.setValue(v.getServiceEntryDate());
        statusBox.setValue(v.getStatus() == null ? VehicleStatus.ACTIVE : v.getStatus());
    }

    private void msg(String s) { msgLabel.setText(s == null ? "" : s); }

    private String statusText(VehicleStatus st) {
        if (st == null) return "";
        return switch (st) {
            case ACTIVE -> "Aktif";
            case IN_SERVICE -> "Bakımda";
            case ASSIGNED -> "Zimmetli";
            case INACTIVE -> "Pasif";
        };
    }
}

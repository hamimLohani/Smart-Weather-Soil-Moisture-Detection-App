package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.*;
import com.agrosense.session.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AccountController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label profileError;

    @FXML private TableView<DevicePairingRow> devicesTable;
    @FXML private TableColumn<DevicePairingRow, String> colSerial, colSite, colStatus, colPaired, colUnpair;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getInstance().getCurrentCustomer();
        nameField.setText(customer.getName());
        phoneField.setText(customer.getPhone() != null ? customer.getPhone() : "");
        emailField.setText(customer.getEmail());

        setupTable();
        loadDevices();
    }

    @FXML
    private void onSaveProfile() {
        Customer customer = SessionManager.getInstance().getCurrentCustomer();
        customer.setName(nameField.getText().trim());
        customer.setPhone(phoneField.getText().trim());
        try {
            // Persist updated name/phone via a fresh CustomerDAO (service layer exposes login/register;
            // profile edits go direct to DAO since there is no dedicated update service method yet).
            var dao = new com.agrosense.dao.CustomerDAO();
            customer.setName(nameField.getText().trim());
            customer.setPhone(phoneField.getText().trim());
            dao.update(customer);
            // Refresh session
            SessionManager.getInstance().setCurrentCustomer(customer);
            profileError.setText("✓ Profile saved.");
            profileError.setStyle("-fx-text-fill: -color-accent;");
            profileError.setVisible(true); profileError.setManaged(true);
        } catch (Exception e) {
            showProfileError("Save failed: " + e.getMessage());
        }
    }

    private void setupTable() {
        colSerial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().serialNumber()));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().siteName()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        colPaired.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().pairedDate()));

        colUnpair.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Unpair");
            { btn.getStyleClass().add("btn-danger"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                DevicePairingRow row = getTableView().getItems().get(getIndex());
                btn.setDisable(!row.active());
                btn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Unpair device " + row.serialNumber() + "?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.YES) {
                            try {
                                AgroSenseApp.deviceService.unpairDevice(row.pairingId());
                                loadDevices();
                            } catch (Exception ex) { ex.printStackTrace(); }
                        }
                    });
                });
                setGraphic(btn);
            }
        });
    }

    private void loadDevices() {
        try {
            int customerId = SessionManager.getInstance().getCurrentCustomer().getId();
            List<DevicePairing> pairings = AgroSenseApp.deviceService.getPairingsForCustomer(customerId);
            List<DevicePairingRow> rows = new ArrayList<>();
            for (DevicePairing p : pairings) {
                String serial = AgroSenseApp.deviceService.getDeviceById(p.getDeviceUnitId())
                    .map(DeviceUnit::getSerialNumber).orElse("?");
                String siteName = AgroSenseApp.siteService.getSiteById(p.getSiteId())
                    .map(Site::getName).orElse("?");
                String status = p.isActive() ? "PAIRED" : "UNPAIRED";
                String paired = p.getPairedDate() != null ? p.getPairedDate().format(FMT) : "?";
                rows.add(new DevicePairingRow(p.getId(), serial, siteName, status, paired, p.isActive()));
            }
            devicesTable.setItems(FXCollections.observableArrayList(rows));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showProfileError(String msg) {
        profileError.setText(msg);
        profileError.setStyle("");
        profileError.setVisible(true); profileError.setManaged(true);
    }

    public record DevicePairingRow(int pairingId, String serialNumber, String siteName,
                                   String status, String pairedDate, boolean active) {}
}

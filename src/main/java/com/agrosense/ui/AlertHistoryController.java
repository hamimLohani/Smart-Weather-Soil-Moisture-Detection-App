package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.*;
import com.agrosense.session.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import java.util.List;

public class AlertHistoryController {

    @FXML private TableView<AlertEvent> alertTable;
    @FXML private TableColumn<AlertEvent, String> colDevice, colRule, colValue, colStatus, colTime, colActions;
    @FXML private ComboBox<String> statusFilter;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("ALL", "OPEN", "ACKNOWLEDGED", "RESOLVED"));
        statusFilter.setValue("ALL");
        setupColumns();
        onRefresh();
    }

    @FXML
    private void onFilterChanged() { onRefresh(); }

    @FXML
    private void onRefresh() {
        try {
            int customerId = SessionManager.getInstance().getCurrentCustomer().getId();
            String filter = statusFilter.getValue();
            List<AlertEvent> events;
            if ("ALL".equals(filter)) {
                events = AgroSenseApp.alertService.getEventsForCustomer(customerId);
            } else {
                events = AgroSenseApp.alertService.getEventsForCustomerByStatus(
                    customerId, AlertEventStatus.valueOf(filter));
            }
            alertTable.setItems(FXCollections.observableArrayList(events));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupColumns() {
        colDevice.setCellValueFactory(c -> {
            try {
                return new SimpleStringProperty(
                    AgroSenseApp.deviceService.getDeviceById(c.getValue().getDeviceUnitId())
                        .map(DeviceUnit::getSerialNumber).orElse("?"));
            } catch (SQLException e) { return new SimpleStringProperty("?"); }
        });

        colRule.setCellValueFactory(c -> new SimpleStringProperty(
            "Rule #" + c.getValue().getAlertRuleId()));

        colValue.setCellValueFactory(c -> new SimpleStringProperty(
            String.format("%.2f", c.getValue().getTriggeredValue())));

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(null); setGraphic(null);
                if (!empty && item != null) {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(switch (item) {
                        case "OPEN"         -> "badge-open";
                        case "ACKNOWLEDGED" -> "badge-acknowledged";
                        case "RESOLVED"     -> "badge-resolved";
                        default             -> "label-secondary";
                    });
                    setGraphic(badge);
                }
            }
        });

        colTime.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : "—"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button ackBtn     = new Button("Acknowledge");
            private final Button resolveBtn = new Button("Resolve");
            private final HBox box = new HBox(6, ackBtn, resolveBtn);
            {
                ackBtn.getStyleClass().add("btn-warn");
                resolveBtn.getStyleClass().add("btn-secondary");

                ackBtn.setOnAction(e -> {
                    AlertEvent event = getTableView().getItems().get(getIndex());
                    try {
                        AgroSenseApp.alertService.acknowledge(event.getId());
                        onRefresh();
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                resolveBtn.setOnAction(e -> {
                    AlertEvent event = getTableView().getItems().get(getIndex());
                    try {
                        AgroSenseApp.alertService.resolve(event.getId());
                        onRefresh();
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AlertEvent event = getTableView().getItems().get(getIndex());
                ackBtn.setDisable(event.getStatus() != AlertEventStatus.OPEN);
                resolveBtn.setDisable(event.getStatus() == AlertEventStatus.RESOLVED);
                setGraphic(box);
            }
        });
    }
}

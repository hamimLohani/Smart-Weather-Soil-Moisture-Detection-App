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
import java.util.List;

public class AlertRulesController {

    @FXML private ComboBox<Site> siteCombo;
    @FXML private TableView<AlertRule> rulesTable;
    @FXML private TableColumn<AlertRule, String> colType, colOperator, colThreshold, colActive, colActions;
    @FXML private ComboBox<ReadingType> typeCombo;
    @FXML private ComboBox<ComparisonOperator> operatorCombo;
    @FXML private TextField thresholdField;
    @FXML private Label formTitle, formError;
    @FXML private javafx.scene.layout.VBox formPane;

    private AlertRule editingRule = null;

    @FXML
    public void initialize() {
        try {
            int customerId = SessionManager.getInstance().getCurrentCustomer().getId();
            List<Site> sites = AgroSenseApp.siteService.getSitesForCustomer(customerId);
            siteCombo.setItems(FXCollections.observableArrayList(sites));

            typeCombo.setItems(FXCollections.observableArrayList(ReadingType.values()));
            operatorCombo.setItems(FXCollections.observableArrayList(ComparisonOperator.values()));
            operatorCombo.setConverter(new javafx.util.StringConverter<>() {
                public String toString(ComparisonOperator op) { return op == null ? "" : op.getSymbol() + " (" + op.name() + ")"; }
                public ComparisonOperator fromString(String s) { return null; }
            });

            setupTableColumns();
            if (!sites.isEmpty()) {
                siteCombo.setValue(sites.get(0));
                onSiteSelected();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void onSiteSelected() {
        Site site = siteCombo.getValue();
        if (site == null) return;
        try {
            List<AlertRule> rules = AgroSenseApp.alertRuleService.getRulesForSite(site.getId());
            rulesTable.setItems(FXCollections.observableArrayList(rules));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void onAddRule() {
        editingRule = null;
        formTitle.setText("Add Alert Rule");
        typeCombo.setValue(null); operatorCombo.setValue(null); thresholdField.clear();
        showForm(true);
    }

    @FXML
    private void onSaveRule() {
        Site site = siteCombo.getValue();
        ReadingType type = typeCombo.getValue();
        ComparisonOperator op = operatorCombo.getValue();
        String threshStr = thresholdField.getText().trim();

        if (site == null || type == null || op == null || threshStr.isEmpty()) {
            showFormError("All fields are required.");
            return;
        }

        double threshold;
        try { threshold = Double.parseDouble(threshStr); }
        catch (NumberFormatException e) { showFormError("Threshold must be a number."); return; }

        try {
            if (editingRule == null) {
                AgroSenseApp.alertRuleService.createRule(site.getId(), type, op, threshold);
            } else {
                editingRule.setReadingType(type);
                editingRule.setComparisonOperator(op);
                editingRule.setThresholdValue(threshold);
                AgroSenseApp.alertRuleService.updateRule(editingRule);
            }
            showForm(false);
            onSiteSelected();
        } catch (SQLException e) { showFormError("Error: " + e.getMessage()); }
    }

    @FXML private void onCancelForm() { showForm(false); }

    private void setupTableColumns() {
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReadingType().name()));
        colOperator.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getComparisonOperator().getSymbol()));
        colThreshold.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getThresholdValue())));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Yes" : "No"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button toggleBtn = new Button("Toggle");
            private final HBox box = new HBox(6, editBtn, toggleBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-secondary");
                deleteBtn.getStyleClass().add("btn-danger");
                toggleBtn.getStyleClass().add("btn-warn");

                editBtn.setOnAction(e -> {
                    AlertRule rule = getTableView().getItems().get(getIndex());
                    editingRule = rule;
                    formTitle.setText("Edit Alert Rule");
                    typeCombo.setValue(rule.getReadingType());
                    operatorCombo.setValue(rule.getComparisonOperator());
                    thresholdField.setText(String.valueOf(rule.getThresholdValue()));
                    showForm(true);
                });

                deleteBtn.setOnAction(e -> {
                    AlertRule rule = getTableView().getItems().get(getIndex());
                    try {
                        AgroSenseApp.alertRuleService.deleteRule(rule.getId());
                        onSiteSelected();
                    } catch (SQLException ex) { ex.printStackTrace(); }
                });

                toggleBtn.setOnAction(e -> {
                    AlertRule rule = getTableView().getItems().get(getIndex());
                    rule.setActive(!rule.isActive());
                    try {
                        AgroSenseApp.alertRuleService.updateRule(rule);
                        onSiteSelected();
                    } catch (SQLException ex) { ex.printStackTrace(); }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void showForm(boolean visible) {
        formPane.setVisible(visible); formPane.setManaged(visible);
        formError.setVisible(false); formError.setManaged(false);
    }

    private void showFormError(String msg) {
        formError.setText(msg); formError.setVisible(true); formError.setManaged(true);
    }
}

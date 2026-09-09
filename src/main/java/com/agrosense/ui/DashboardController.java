package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.*;
import com.agrosense.session.SessionManager;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;

import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML private FlowPane sitesContainer;
    @FXML private Label subtitleLabel;
    @FXML private Label alertCountLabel;
    @FXML private ListView<String> recentAlertsList;



    @FXML
    public void initialize() {
        Customer customer = SessionManager.getInstance().getCurrentCustomer();
        subtitleLabel.setText("Welcome, " + customer.getName());
        loadSites();
        refreshAlertCount();

        // Bind live alert list from Observer
        AgroSenseApp.liveAlerts.addListener((ListChangeListener<AlertEvent>) change -> {
            Platform.runLater(() -> {
                refreshAlertCount();
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (AlertEvent e : change.getAddedSubList()) {
                            recentAlertsList.getItems().add(0,
                                "⚠ Alert #" + e.getId() + " — value=" +
                                String.format("%.2f", e.getTriggeredValue()));
                        }
                    }
                }
            });
        });
    }

    @FXML
    private void onPairDevice() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showPairing();
        }
    }

    private void loadSites() {
        sitesContainer.getChildren().clear();
        try {
            int customerId = SessionManager.getInstance().getCurrentCustomer().getId();
            List<Site> sites = AgroSenseApp.siteService.getSitesForCustomer(customerId);

            if (sites.isEmpty()) {
                Label empty = new Label("No sites yet — pair your first device to get started! 🌱");
                empty.getStyleClass().add("label-secondary");
                sitesContainer.getChildren().add(empty);
                return;
            }

            for (Site site : sites) {
                sitesContainer.getChildren().add(buildSiteCard(site));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildSiteCard(Site site) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(16));

        // Site name + profile badge
        HBox header = new HBox(8);
        Label nameLabel = new Label(site.getName());
        nameLabel.getStyleClass().add("title-medium");
        Label profileBadge = new Label(site.getUseCaseProfile() == UseCaseProfile.FARM ? "🌾 FARM" : "🏡 HOME");
        profileBadge.getStyleClass().add(site.getUseCaseProfile() == UseCaseProfile.FARM ? "badge-farm" : "badge-home");
        header.getChildren().addAll(nameLabel, profileBadge);

        // FARM attention badge
        boolean needsAttention = AgroSenseApp.alertService.getSitesNeedingAttention().contains(site.getId());
        if (needsAttention) {
            Label attention = new Label("⚠ Needs Attention");
            attention.getStyleClass().add("badge-open");
            header.getChildren().add(attention);
        }

        // Latest readings
        GridPane metrics = new GridPane();
        metrics.setHgap(16); metrics.setVgap(8);
        addMetric(metrics, 0, "🌡 Temp", getLatestReading(site, ReadingType.TEMPERATURE), "°C");
        addMetric(metrics, 1, "💧 Humidity", getLatestReading(site, ReadingType.HUMIDITY), "%");
        addMetric(metrics, 2, "🌱 Soil", getLatestReading(site, ReadingType.SOIL_MOISTURE), "%");

        // Open alerts
        int openAlerts = getOpenAlertCount(site);
        Label alertLabel = openAlerts > 0
            ? labelWithStyle("🔔 " + openAlerts + " open alert" + (openAlerts > 1 ? "s" : ""), "badge-open")
            : labelWithStyle("✓ No open alerts", "badge-resolved");

        // Detail button
        Button detailBtn = new Button("View Details →");
        detailBtn.getStyleClass().add("btn-secondary");
        detailBtn.setOnAction(e -> openSiteDetail(site));

        card.getChildren().addAll(header, new Separator(), metrics, alertLabel, detailBtn);
        return card;
    }

    private void addMetric(GridPane grid, int row, String label, String value, String unit) {
        Label lbl = new Label(label); lbl.getStyleClass().add("metric-label");
        Label val = new Label(value + unit); val.getStyleClass().add("text-accent");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private String getLatestReading(Site site, ReadingType type) {
        try {
            List<DevicePairing> pairings = AgroSenseApp.deviceService.getPairingsForCustomer(
                SessionManager.getInstance().getCurrentCustomer().getId());
            for (DevicePairing p : pairings) {
                if (p.getSiteId() == site.getId() && p.isActive()) {
                    return AgroSenseApp.sensorReadingService.getLatest(p.getDeviceUnitId(), type)
                        .map(r -> String.format("%.1f", r.getValue()))
                        .orElse("—");
                }
            }
        } catch (SQLException e) { /* ignore */ }
        return "—";
    }

    private int getOpenAlertCount(Site site) {
        try {
            List<DevicePairing> pairings = AgroSenseApp.deviceService.getPairingsForCustomer(
                SessionManager.getInstance().getCurrentCustomer().getId());
            int total = 0;
            for (DevicePairing p : pairings) {
                if (p.getSiteId() == site.getId() && p.isActive()) {
                    total += AgroSenseApp.alertService.countOpenAlerts(p.getDeviceUnitId());
                }
            }
            return total;
        } catch (SQLException e) { return 0; }
    }

    private void refreshAlertCount() {
        int total = AgroSenseApp.liveAlerts.size();
        alertCountLabel.setText(total > 0 ? total + " live alert" + (total > 1 ? "s" : "") : "");
        alertCountLabel.setVisible(total > 0);
        alertCountLabel.setManaged(total > 0);
    }

    private void openSiteDetail(Site site) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/site_detail.fxml"));
            Parent view = loader.load();
            SiteDetailController ctrl = loader.getController();
            ctrl.setSite(site);
            sitesContainer.getScene().setRoot(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Label labelWithStyle(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }
}

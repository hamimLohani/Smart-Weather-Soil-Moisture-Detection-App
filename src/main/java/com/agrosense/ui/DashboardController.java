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
import javafx.scene.paint.Color;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML private FlowPane sitesContainer;
    @FXML private Label subtitleLabel;
    @FXML private Label alertCountLabel;
    @FXML private ListView<String> recentAlertsList;



    private Timeline refreshTimeline;

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
        
        // Auto-refresh the dashboard every 2 seconds for real-time updates
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> loadSites()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        
        // Stop timeline if node is removed from scene
        sitesContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && refreshTimeline != null) {
                refreshTimeline.stop();
            }
        });
    }

    @FXML
    private void onPairDevice() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showPairing();
        }
    }

    @FXML
    private void onReload() {
        loadSites();
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
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label name = new Label(site.getName());
        name.getStyleClass().add("card-title");
        
        java.util.Optional<Integer> activeDeviceOpt = getActiveDevice(site);
        boolean hasActiveDevice = activeDeviceOpt.isPresent();

        Label profileBadge = new Label(site.getUseCaseProfile().name());
        profileBadge.getStyleClass().addAll("badge", 
            hasActiveDevice ? "badge-profile" : "badge-resolved");
            
        header.getChildren().addAll(name, profileBadge);

        if (!hasActiveDevice) {
            Label unpairedBadge = new Label("Unpaired");
            unpairedBadge.getStyleClass().addAll("badge", "badge-resolved");
            unpairedBadge.setTextFill(Color.GRAY);
            header.getChildren().add(unpairedBadge);
        } else {
            int deviceId = activeDeviceOpt.get();
            boolean isOnline = AgroSenseApp.sensorReadingService.isOnline(deviceId);
            Label statusBadge = new Label(isOnline ? "🟢 Online" : "🔴 Offline");
            statusBadge.getStyleClass().addAll("badge", isOnline ? "badge-resolved" : "badge-open");
            header.getChildren().add(statusBadge);
            
            if (site.getUseCaseProfile() == UseCaseProfile.HOME) {
                Label attention = labelWithStyle("🏡 HOME", "badge-profile");
                header.getChildren().add(attention);
            }
        }

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
        addMetric(metrics, 2, "🌱 Soil Moisture", getLatestReading(site, ReadingType.SOIL_MOISTURE), "%");

        // Open alerts
        int openAlerts = getOpenAlertCount(site);
        Label alertLabel = openAlerts > 0
            ? labelWithStyle("🔔 " + openAlerts + " open alert" + (openAlerts > 1 ? "s" : ""), "badge-open")
            : labelWithStyle("✓ No open alerts", "badge-resolved");

        // Detail button
        Button detailBtn = new Button("View Details →");
        detailBtn.getStyleClass().add("btn-secondary");
        detailBtn.setOnAction(e -> openSiteDetail(site));
        
        HBox buttonBox = new HBox(10, detailBtn);
        
        if (!hasActiveDevice) {
            Button deleteBtn = new Button("Delete Site");
            deleteBtn.getStyleClass().add("btn-secondary");
            deleteBtn.setStyle("-fx-text-fill: #ff6b6b;");
            deleteBtn.setOnAction(e -> deleteSite(site));
            buttonBox.getChildren().add(deleteBtn);
        }

        card.getChildren().addAll(header, new Separator(), metrics, alertLabel, buttonBox);
        return card;
    }
    
    private java.util.Optional<Integer> getActiveDevice(Site site) {
        try {
            List<DevicePairing> pairings = AgroSenseApp.deviceService.getPairingsForCustomer(
                SessionManager.getInstance().getCurrentCustomer().getId());
            for (DevicePairing p : pairings) {
                if (p.getSiteId() == site.getId() && p.isActive()) {
                    return java.util.Optional.of(p.getDeviceUnitId());
                }
            }
        } catch (SQLException e) { /* ignore */ }
        return java.util.Optional.empty();
    }
    
    private void deleteSite(Site site) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to completely delete site '" + site.getName() + "' and its history?", 
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.YES) {
                try {
                    // This relies on the database ON DELETE CASCADE or the service to clean up
                    AgroSenseApp.siteService.deleteSite(site.getId());
                    loadSites();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
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

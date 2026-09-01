package com.agrosense;

import com.agrosense.dao.*;
import com.agrosense.ingest.SensorIngestServer;
import com.agrosense.model.AlertEvent;
import com.agrosense.observer.DashboardAlertObserver;
import com.agrosense.observer.LogAlertObserver;
import com.agrosense.service.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * JavaFX application entry point.
 * Wires all services, starts the ingest server, and loads the login screen.
 */
public class AgroSenseApp extends Application {

    private static final Logger LOG = Logger.getLogger(AgroSenseApp.class.getName());

    // Shared services — accessible via AppContext
    public static AuthService authService;
    public static PairingService pairingService;
    public static SiteService siteService;
    public static SensorReadingService sensorReadingService;
    public static AlertRuleService alertRuleService;
    public static AlertService alertService;
    public static DeviceService deviceService;

    // Live alert list bound to the Dashboard
    public static final ObservableList<AlertEvent> liveAlerts =
        FXCollections.observableArrayList();

    private static Stage primaryStage;
    private SensorIngestServer ingestServer;

    @Override
    public void init() {
        // Initialize DB schema + seed
        com.agrosense.dao.Seeder.initializeAndSeed();

        // DAOs
        var customerDAO       = new CustomerDAO();
        var siteDAO           = new SiteDAO();
        var deviceUnitDAO     = new DeviceUnitDAO();
        var pairingCodeDAO    = new PairingCodeDAO();
        var devicePairingDAO  = new DevicePairingDAO();
        var sensorReadingDAO  = new SensorReadingDAO();
        var alertRuleDAO      = new AlertRuleDAO();
        var alertEventDAO     = new AlertEventDAO();

        // Services
        sensorReadingService = new SensorReadingService(sensorReadingDAO);
        alertService = new AlertService(alertRuleDAO, alertEventDAO, devicePairingDAO, siteDAO);

        // Wire observers
        alertService.addObserver(new DashboardAlertObserver(liveAlerts));
        alertService.addObserver(new LogAlertObserver());

        authService     = new AuthService(customerDAO);
        pairingService  = new PairingService(pairingCodeDAO, deviceUnitDAO, devicePairingDAO, siteDAO);
        siteService     = new SiteService(siteDAO);
        alertRuleService= new AlertRuleService(alertRuleDAO);
        deviceService   = new DeviceService(deviceUnitDAO, devicePairingDAO);

        // Ingest server
        ingestServer = new SensorIngestServer(deviceUnitDAO, devicePairingDAO,
            sensorReadingService, alertService);
        try {
            ingestServer.start();
        } catch (IOException e) {
            LOG.severe("[AgroSenseApp] Failed to start ingest server: " + e.getMessage());
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("AgroSense");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        navigateTo("login");
        stage.show();
    }

    @Override
    public void stop() {
        if (ingestServer != null) ingestServer.stop();
    }

    // ── Navigation ────────────────────────────────────────────────────

    public static void navigateTo(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                AgroSenseApp.class.getResource("/fxml/" + fxmlName + ".fxml"));
            Parent root = loader.load();
            com.agrosense.ui.ThemeManager.applyTheme(root);
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1200, 760);
                scene.getStylesheets().add(
                    AgroSenseApp.class.getResource("/css/agrosense.css").toExternalForm());
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            LOG.severe("[AgroSenseApp] Navigation failed to " + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}

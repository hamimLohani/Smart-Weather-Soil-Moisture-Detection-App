package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.*;
import com.agrosense.session.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SiteDetailController {

    @FXML private Label siteNameLabel;
    @FXML private Label siteProfileLabel;
    @FXML private Label tempLabel;
    @FXML private Label humLabel;
    @FXML private Label soilLabel;
    @FXML private LineChart<String, Number> readingsChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;

    private Site site;
    private int deviceUnitId = -1;
    private static final DateTimeFormatter FMT_AXIS = DateTimeFormatter.ofPattern("MM/dd HH:mm");


    public void setSite(Site site) {
        this.site = site;
        siteNameLabel.setText(site.getName());
        siteProfileLabel.setText(site.getUseCaseProfile().name() + " profile");

        // Default date range: last 7 days
        fromDate.setValue(LocalDate.now().minusDays(7));
        toDate.setValue(LocalDate.now());

        resolveDeviceId();
        loadLatestReadings();
        onLoadChart();
    }

    private void resolveDeviceId() {
        try {
            int customerId = SessionManager.getInstance().getCurrentCustomer().getId();
            List<DevicePairing> pairings = AgroSenseApp.deviceService.getPairingsForCustomer(customerId);
            for (DevicePairing p : pairings) {
                if (p.getSiteId() == site.getId() && p.isActive()) {
                    deviceUnitId = p.getDeviceUnitId();
                    break;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadLatestReadings() {
        if (deviceUnitId == -1) return;
        try {
            tempLabel.setText(fmt(ReadingType.TEMPERATURE));
            humLabel.setText(fmt(ReadingType.HUMIDITY));
            soilLabel.setText(fmt(ReadingType.SOIL_MOISTURE));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String fmt(ReadingType type) throws SQLException {
        return AgroSenseApp.sensorReadingService.getLatest(deviceUnitId, type)
            .map(r -> String.format("%.1f", r.getValue()))
            .orElse("—");
    }

    @FXML
    private void onLoadChart() {
        if (deviceUnitId == -1) return;
        readingsChart.getData().clear();

        LocalDateTime from = fromDate.getValue().atStartOfDay();
        LocalDateTime to   = toDate.getValue().atTime(23, 59, 59);

        try {
            addSeries(ReadingType.TEMPERATURE, from, to, "Temperature (°C)");
            addSeries(ReadingType.HUMIDITY,    from, to, "Humidity (%)");
            addSeries(ReadingType.SOIL_MOISTURE, from, to, "Soil Moisture (%)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addSeries(ReadingType type, LocalDateTime from, LocalDateTime to,
                           String seriesName) throws SQLException {
        List<SensorReading> readings = AgroSenseApp.sensorReadingService
            .getHistory(deviceUnitId, type, from, to);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        for (SensorReading r : readings) {
            String timeLabel = r.getTimestamp() != null ? r.getTimestamp().format(FMT_AXIS) : "?";
            series.getData().add(new XYChart.Data<>(timeLabel, r.getValue()));
        }
        readingsChart.getData().add(series);
    }

    @FXML
    private void goBack() {
        AgroSenseApp.navigateTo("main_layout");
    }
}

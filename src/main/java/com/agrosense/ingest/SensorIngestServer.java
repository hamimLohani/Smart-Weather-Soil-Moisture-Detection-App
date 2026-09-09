package com.agrosense.ingest;

import com.agrosense.dao.DevicePairingDAO;
import com.agrosense.dao.DeviceUnitDAO;
import com.agrosense.dao.PairingCodeDAO;

import com.agrosense.model.*;
import com.agrosense.service.AlertService;
import com.agrosense.service.SensorReadingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Embedded HTTP server that receives sensor POSTs from ESP8266 devices.
 *
 * POST /api/readings
 * Body: {"device_id":"SM-0001","temperature":27.0,"humidity":65,"soil_moisture_raw":480}
 *
 * Flow:
 *  1. Parse JSON
 *  2. Look up DeviceUnit by serial_number
 *  3. Verify active DevicePairing (403 if unpaired)
 *  4. Convert soil_moisture_raw → % using device calibration
 *  5. Insert 3 SensorReading rows
 *  6. Call AlertService.evaluate()
 */
public class SensorIngestServer {

    private static final Logger LOG = Logger.getLogger(SensorIngestServer.class.getName());
    private static final String CONFIG_FILE = "/agrosense.properties";
    private static final int DEFAULT_PORT = 8080;

    private final DeviceUnitDAO deviceUnitDAO;
    private final DevicePairingDAO devicePairingDAO;
    private final SensorReadingService sensorReadingService;
    private final AlertService alertService;
    private final PairingCodeDAO pairingCodeDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpServer server;

    public SensorIngestServer(DeviceUnitDAO deviceUnitDAO,
                               DevicePairingDAO devicePairingDAO,
                               SensorReadingService sensorReadingService,
                               AlertService alertService,
                               PairingCodeDAO pairingCodeDAO) {
        this.deviceUnitDAO = deviceUnitDAO;
        this.devicePairingDAO = devicePairingDAO;
        this.sensorReadingService = sensorReadingService;
        this.alertService = alertService;
        this.pairingCodeDAO = pairingCodeDAO;
    }

    public void start() throws IOException {
        int port = readPort();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/readings", new ReadingsHandler());
        server.setExecutor(null);
        server.start();
        LOG.info("[SensorIngestServer] Listening on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            LOG.info("[SensorIngestServer] Stopped.");
        }
    }

    private int readPort() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            LOG.warning("[SensorIngestServer] Could not read config, using default port.");
        }
        return Integer.parseInt(props.getProperty("ingest.server.port", String.valueOf(DEFAULT_PORT)));
    }

    // ── inner handler ─────────────────────────────────────────────────────────

    private class ReadingsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"Method not allowed. Use POST.\"}" );
                return;
            }

            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // --- Step 1: Parse JSON ---
            JsonNode json;
            try {
                json = mapper.readTree(body);
                if (json == null || !json.isObject()) {
                    respond(exchange, 400, "{\"error\":\"Invalid JSON body.\"}" );
                    return;
                }
            } catch (Exception e) {
                LOG.warning("[Ingest] Malformed JSON received: " + e.getMessage());
                respond(exchange, 400, "{\"error\":\"Malformed JSON body.\"}" );
                return;
            }

            // --- Step 2: Validate required fields ---
            if (json.get("device_id") == null || json.get("device_id").asText().isBlank()) {
                respond(exchange, 400, "{\"error\":\"Missing required field: device_id.\"}" );
                return;
            }
            if (json.get("temperature") == null) {
                respond(exchange, 400, "{\"error\":\"Missing required field: temperature.\"}" );
                return;
            }
            if (json.get("humidity") == null) {
                respond(exchange, 400, "{\"error\":\"Missing required field: humidity.\"}" );
                return;
            }
            if (json.get("soil_moisture_raw") == null) {
                respond(exchange, 400, "{\"error\":\"Missing required field: soil_moisture_raw.\"}" );
                return;
            }

            // --- Step 3: Extract values ---
            try {
                String deviceId    = json.get("device_id").asText();
                double temperature = json.get("temperature").asDouble();
                double humidity    = json.get("humidity").asDouble();
                int soilRaw        = json.get("soil_moisture_raw").asInt();

                // 1. Look up device
                Optional<DeviceUnit> deviceOpt = deviceUnitDAO.findBySerialNumber(deviceId);
                
                // AUTO-REGISTRATION: If device doesn't exist and firmware provided pairing_code, register it!
                if (deviceOpt.isEmpty()) {
                    if (json.has("pairing_code") && !json.get("pairing_code").asText().isBlank()) {
                        String pairingCode = json.get("pairing_code").asText();
                        int newUnitId = deviceUnitDAO.insert(deviceId);
                        pairingCodeDAO.insert(pairingCode, newUnitId);
                        LOG.info("[Ingest] Auto-registered new device: " + deviceId);
                        deviceOpt = deviceUnitDAO.findById(newUnitId);
                    } else {
                        respond(exchange, 404, "{\"error\":\"Device not found.\"}" );
                        return;
                    }
                }
                DeviceUnit device = deviceOpt.get();

                // 2. Verify active pairing (403 if unpaired)
                Optional<?> pairingOpt = devicePairingDAO.findActiveByDeviceUnit(device.getId());
                if (pairingOpt.isEmpty()) {
                    respond(exchange, 403, "{\"error\":\"Device is not paired to any customer.\"}" );
                    return;
                }

                // 3. Convert soil moisture raw → percentage
                double soilPercent = device.convertSoilMoistureToPercent(soilRaw);

                // 4. Persist readings
                sensorReadingService.saveReading(device.getId(), ReadingType.TEMPERATURE, temperature);
                sensorReadingService.saveReading(device.getId(), ReadingType.HUMIDITY, humidity);
                sensorReadingService.saveReading(device.getId(), ReadingType.SOIL_MOISTURE, soilPercent);

                // Build list for alert evaluation
                List<SensorReading> readings = new ArrayList<>();
                readings.add(makeReading(device.getId(), ReadingType.TEMPERATURE, temperature));
                readings.add(makeReading(device.getId(), ReadingType.HUMIDITY, humidity));
                readings.add(makeReading(device.getId(), ReadingType.SOIL_MOISTURE, soilPercent));

                // 5. Update last_seen
                deviceUnitDAO.updateLastSeen(device.getId());

                // 6. Evaluate alerts
                alertService.evaluate(device, readings);

                LOG.info(String.format("[Ingest] %s — temp=%.1f°C hum=%.1f%% soil=%.1f%%",
                    deviceId, temperature, humidity, soilPercent));

                respond(exchange, 200, "{\"status\":\"ok\"}");

            } catch (Exception e) {
                LOG.severe("[Ingest] Unexpected error processing reading: " + e.getMessage());
                respond(exchange, 500, "{\"error\":\"Internal server error. Please try again.\"}" );
            }
        }

        private void respond(HttpExchange ex, int code, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private SensorReading makeReading(int deviceId, ReadingType type, double value) {
            SensorReading r = new SensorReading();
            r.setDeviceUnitId(deviceId);
            r.setReadingType(type);
            r.setValue(value);
            return r;
        }
    }
}

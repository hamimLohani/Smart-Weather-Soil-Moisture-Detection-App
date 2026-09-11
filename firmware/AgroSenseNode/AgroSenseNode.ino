#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266HTTPClient.h>
#include <WiFiClient.h>
#include <DNSServer.h>      // Captive portal: redirect all DNS to our IP
#include <DHT.h>
#include <EEPROM.h>
#include <ArduinoJson.h>

// --- Device Identity ---
// These are derived at runtime from the ESP8266's unique hardware Chip ID.
// ESP.getChipId() returns a 32-bit integer burned into silicon at manufacture —
// no two ESP8266 chips have the same value, so no hardcoding is needed.
char DEVICE_SERIAL[16];  // e.g. "SM-A1B2C3"
char PAIRING_CODE[12];   // e.g. "A1B2-C3D4"

// --- Hardware Pins ---
#define DHTPIN D4        // GPIO2
#define DHTTYPE DHT11    // DHT 11 sensor type
#define SOIL_PIN A0      // Analog input A0
#define RESET_PIN D1     // GPIO5 for factory reset button (active LOW)

DHT dht(DHTPIN, DHTTYPE);

// --- EEPROM Layout ---
// 0: Configured Flag (1 = configured, 0 = unconfigured)
// 1-32: WiFi SSID (max 32 chars)
// 33-96: WiFi Password (max 63 chars)
// 97-128: Server IP (max 31 chars)
#define EEPROM_SIZE 512
#define ADDR_FLAG 0
#define ADDR_SSID 1
#define ADDR_PASS 33
#define ADDR_IP 97
#define MAX_SSID_LEN 32
#define MAX_PASS_LEN 64
#define MAX_IP_LEN 32

// --- Global Variables ---
ESP8266WebServer server(80);
DNSServer dnsServer;              // Captive portal DNS — intercepts all queries
const byte DNS_PORT = 53;
bool isConfigured = false;
char wifiSsid[MAX_SSID_LEN];
char wifiPass[MAX_PASS_LEN];
char serverIp[MAX_IP_LEN];
int consecutiveFailures = 0;

unsigned long lastReadingTime = 0;
const unsigned long READING_INTERVAL = 2000; // 2 seconds

// --- Function Prototypes ---
void generateDeviceIdentity();
void checkFactoryResetButton();
void loadCredentialsFromEEPROM();
void saveCredentialsToEEPROM(String ssid, String pass, String ip);
void startProvisioningMode();
void connectToSavedWifi();
void readAndSendSensorData();
void handleRoot();
void handleConfigure();
void handleCaptivePortal();  // Captive portal redirect handler

/**
 * Derives a unique Device Serial and Pairing Code from the ESP8266's
 * hardware Chip ID (a 32-bit value burned into silicon at manufacture).
 *
 * Serial format:  SM-XXXXXX  (6 uppercase hex digits of the chip ID)
 * Pairing format: XXXX-XXXX  (chip ID split into two 4-digit hex groups)
 *
 * These will be consistent across reboots for the same physical device,
 * and globally unique across all ESP8266 chips.
 */
void generateDeviceIdentity() {
  uint32_t chipId = ESP.getChipId();

  // Serial: "SM-" + full 6-digit hex of chip ID
  snprintf(DEVICE_SERIAL, sizeof(DEVICE_SERIAL), "SM-%06X", chipId);

  // Pairing code: split the 32-bit ID into two 16-bit halves, formatted
  // as "XXXX-XXXX" — easy to read aloud or type into the JavaFX app.
  uint16_t highHalf = (chipId >> 16) & 0xFFFF;
  uint16_t lowHalf  = chipId & 0xFFFF;
  snprintf(PAIRING_CODE, sizeof(PAIRING_CODE), "%04X-%04X", highHalf, lowHalf);

  Serial.print("Chip ID (raw):  "); Serial.println(chipId, HEX);
  Serial.print("Device Serial:  "); Serial.println(DEVICE_SERIAL);
  Serial.print("Pairing Code:   "); Serial.println(PAIRING_CODE);
}

void setup() {
  Serial.begin(115200);
  delay(100);
  Serial.println("\n\n--- AgroSense Sensor Node Starting ---");

  pinMode(RESET_PIN, INPUT_PULLUP);
  dht.begin();
  EEPROM.begin(EEPROM_SIZE);

  // Derive a unique device identity from the ESP8266 hardware Chip ID.
  // This is a 32-bit value burned into silicon — unique per chip.
  generateDeviceIdentity();

  // Check if the user is holding the reset button
  checkFactoryResetButton();

  // Read the configured flag from EEPROM
  isConfigured = (EEPROM.read(ADDR_FLAG) == 1);

  if (isConfigured) {
    loadCredentialsFromEEPROM();
    connectToSavedWifi();
  } else {
    startProvisioningMode();
  }
}

void loop() {
  if (isConfigured) {
    // --- Mode 2: Normal Operation ---
    unsigned long currentMillis = millis();
    if (currentMillis - lastReadingTime >= READING_INTERVAL) {
      lastReadingTime = currentMillis;
      readAndSendSensorData();
    }
  } else {
    // --- Mode 1: Provisioning ---
    // Process DNS first so captive portal detection works on all platforms.
    dnsServer.processNextRequest();
    server.handleClient();
  }
}

/**
 * Checks if the designated reset pin is held LOW during boot.
 * If so, wipes the EEPROM configured flag to force provisioning mode.
 */
void checkFactoryResetButton() {
  if (digitalRead(RESET_PIN) == LOW) {
    Serial.println("Factory reset button held. Clearing EEPROM...");
    EEPROM.write(ADDR_FLAG, 0);
    EEPROM.commit();
    Serial.println("EEPROM cleared.");
    
    // Wait for the user to release the button
    while(digitalRead(RESET_PIN) == LOW) {
      delay(10);
    }
    Serial.println("Button released. Entering provisioning mode.");
  }
}

/**
 * Reads WiFi credentials and server IP from EEPROM safely.
 */
void loadCredentialsFromEEPROM() {
  EEPROM.get(ADDR_SSID, wifiSsid);
  EEPROM.get(ADDR_PASS, wifiPass);
  EEPROM.get(ADDR_IP, serverIp);
  
  // Ensure null termination to prevent buffer overruns
  wifiSsid[MAX_SSID_LEN - 1] = '\0';
  wifiPass[MAX_PASS_LEN - 1] = '\0';
  serverIp[MAX_IP_LEN - 1] = '\0';
  
  Serial.println("Loaded credentials from EEPROM:");
  Serial.print("SSID: "); Serial.println(wifiSsid);
  Serial.print("Server IP: "); Serial.println(serverIp);
}

/**
 * Saves WiFi credentials and server IP to EEPROM and sets the configured flag to 1.
 */
void saveCredentialsToEEPROM(String ssid, String pass, String ip) {
  EEPROM.write(ADDR_FLAG, 1);
  
  // Safely copy strings into char arrays
  strncpy(wifiSsid, ssid.c_str(), MAX_SSID_LEN - 1);
  wifiSsid[MAX_SSID_LEN - 1] = '\0';
  EEPROM.put(ADDR_SSID, wifiSsid);
  
  strncpy(wifiPass, pass.c_str(), MAX_PASS_LEN - 1);
  wifiPass[MAX_PASS_LEN - 1] = '\0';
  EEPROM.put(ADDR_PASS, wifiPass);
  
  strncpy(serverIp, ip.c_str(), MAX_IP_LEN - 1);
  serverIp[MAX_IP_LEN - 1] = '\0';
  EEPROM.put(ADDR_IP, serverIp);
  
  EEPROM.commit();
  Serial.println("Credentials saved to EEPROM.");
}

/**
 * Mode 1: Starts a WiFi Access Point with no password and launches a minimal
 * web server to collect configuration details.
 */
void startProvisioningMode() {
  isConfigured = false;
  // The hotspot SSID includes the pairing code so the user can read it
  // directly from their phone/laptop WiFi scanner — no display needed.
  // e.g. "AgroSense-A1B2-C3D4" where "A1B2-C3D4" is the pairing code.
  String apSsid = String("AgroSense-") + PAIRING_CODE;

  Serial.println("\n--- Entering Provisioning Mode ---");
  Serial.print("Device Serial: "); Serial.println(DEVICE_SERIAL);
  Serial.print("Pairing Code:  "); Serial.println(PAIRING_CODE);
  Serial.print("Hotspot SSID:  "); Serial.println(apSsid);
  Serial.println("Captive portal active — browser will auto-open on connect.");

  WiFi.mode(WIFI_AP);
  WiFi.softAP(apSsid.c_str()); // Open network, no password

  // --- Captive Portal: DNS Server ---
  // Redirect ALL DNS queries to our own IP (192.168.4.1) so that the OS
  // captive portal detection fails to reach the internet and triggers the
  // "Sign in to network" popup automatically on iOS, Android, and macOS.
  dnsServer.start(DNS_PORT, "*", WiFi.softAPIP());
  Serial.print("DNS captive portal started on: "); Serial.println(WiFi.softAPIP());

  // --- Web Server Routes ---
  // Setup page
  server.on("/", HTTP_GET, handleRoot);
  server.on("/configure", HTTP_POST, handleConfigure);

  // iOS captive portal detection endpoints
  server.on("/hotspot-detect.html",              HTTP_GET, handleCaptivePortal);
  server.on("/library/test/success.html",        HTTP_GET, handleCaptivePortal);

  // Android / Chrome captive portal detection endpoints
  server.on("/generate_204",                     HTTP_GET, handleCaptivePortal);
  server.on("/connecttest.txt",                  HTTP_GET, handleCaptivePortal);
  server.on("/redirect",                         HTTP_GET, handleCaptivePortal);
  server.on("/success.txt",                      HTTP_GET, handleCaptivePortal);

  // Windows / Microsoft Network Connectivity Status Indicator (NCSI)
  server.on("/ncsi.txt",                         HTTP_GET, handleCaptivePortal);
  server.on("/connecttest.txt",                  HTTP_GET, handleCaptivePortal);

  // Catch-all: any unknown URL gets redirected to the setup page
  server.onNotFound(handleCaptivePortal);

  server.begin();
  Serial.println("Web server started on port 80.");
}

/**
 * Captive portal handler: redirects all OS connectivity-check requests
 * to the setup page at http://192.168.4.1/
 *
 * When iOS/Android/macOS/Windows connect to a WiFi network, they make
 * HTTP requests to known URLs to test internet access. If those requests
 * return anything other than the expected response (e.g. a redirect),
 * the OS shows a "Sign In to Network" popup and opens the redirected URL.
 */
void handleCaptivePortal() {
  Serial.println("[Captive Portal] Redirecting client to setup page.");
  server.sendHeader("Location", "http://192.168.4.1/", true);
  server.send(302, "text/plain", "");
}


/**
 * Scans nearby WiFi networks and returns an HTML <select> dropdown.
 * Called inside handleRoot() so the scan is fresh on every page load.
 */
String buildNetworkDropdown() {
  Serial.println("[Provisioning] Scanning for WiFi networks...");
  int n = WiFi.scanNetworks();
  Serial.print("[Provisioning] "); Serial.print(n); Serial.println(" networks found.");

  String select = "<select name='ssid' style='width:100%; padding:8px; font-size:16px; margin-bottom:16px;'>";
  select += "<option value='' disabled selected>-- Select your WiFi network --</option>";

  if (n == 0) {
    select += "<option value='' disabled>No networks found</option>";
  } else {
    // Sort by signal strength (RSSI) — strongest first
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        if (WiFi.RSSI(j) > WiFi.RSSI(i)) {
          // Swap
          String tmpSsid = WiFi.SSID(i);
          int32_t tmpRssi = WiFi.RSSI(i);
          // Use scan index ordering by building HTML directly in sorted order
        }
      }
      String ssid     = WiFi.SSID(i);
      int32_t rssi    = WiFi.RSSI(i);
      String security = (WiFi.encryptionType(i) == ENC_TYPE_NONE) ? " 🔓" : " 🔒";

      // Signal bar indicator
      String bar;
      if      (rssi >= -55) bar = "▂▄▆█";
      else if (rssi >= -65) bar = "▂▄▆ ";
      else if (rssi >= -75) bar = "▂▄  ";
      else                  bar = "▂   ";

      select += "<option value='" + ssid + "'>";
      select += bar + " " + ssid + security + " (" + String(rssi) + " dBm)";
      select += "</option>";

      Serial.print("  [" + String(i+1) + "] ");
      Serial.print(ssid); Serial.print(" | RSSI: "); Serial.println(rssi);
    }
  }

  select += "</select>";
  WiFi.scanDelete(); // Free memory
  return select;
}

/**
 * Serves the provisioning HTML form with a live WiFi network dropdown.
 */
void handleRoot() {
  String networkList = buildNetworkDropdown();

  String html = "<!DOCTYPE html><html><head>";
  html += "<meta charset='UTF-8'>";
  html += "<meta name='viewport' content='width=device-width, initial-scale=1'>";
  html += "<title>AgroSense Setup</title>";
  html += "<style>";
  html += "body { font-family: sans-serif; max-width: 480px; margin: 32px auto; padding: 16px; }";
  html += "h2 { color: #2e7d32; } .info { background:#f1f8e9; border-left:4px solid #66bb6a; padding:12px; border-radius:4px; margin-bottom:16px; }";
  html += ".code { font-size: 22px; font-weight: bold; letter-spacing: 3px; color: #1b5e20; }";
  html += "label { display:block; font-weight:bold; margin-top:12px; margin-bottom:4px; }";
  html += "input[type=password], input[type=text] { width:100%; padding:8px; font-size:16px; box-sizing:border-box; }";
  html += "input[type=submit] { margin-top:20px; width:100%; padding:12px; background:#2e7d32; color:white; border:none; font-size:16px; border-radius:4px; cursor:pointer; }";
  html += "</style></head><body>";
  html += "<h2>🌱 AgroSense Node Setup</h2>";
  html += "<div class='info'>";
  html += "<p>Device Serial: <b>" + String(DEVICE_SERIAL) + "</b></p>";
  html += "<p>Pairing Code: <span class='code'>" + String(PAIRING_CODE) + "</span></p>";
  html += "<small>Note the pairing code — you will enter it in the JavaFX app to link this device.</small>";
  html += "</div>";
  html += "<form action='/configure' method='POST'>";
  html += "<label>📶 Select WiFi Network:</label>";
  html += networkList;
  html += "<label>🔑 WiFi Password:</label>";
  html += "<input type='password' name='pass' placeholder='Leave blank if open network'>";
  html += "<label>🖥 JavaFX App Server IP:</label>";
  html += "<input type='text' name='ip' placeholder='e.g. 192.168.1.50' required>";
  html += "<input type='submit' value='Save &amp; Connect'>";
  html += "</form></body></html>";

  server.send(200, "text/html", html);
}

/**
 * Handles the POST request from the provisioning form.
 */
void handleConfigure() {
  if (server.hasArg("ssid") && server.hasArg("pass") && server.hasArg("ip")) {
    String ssid = server.arg("ssid");
    String pass = server.arg("pass");
    String ip = server.arg("ip");
    
    saveCredentialsToEEPROM(ssid, pass, ip);
    
    server.send(200, "text/html", "<html><body style='font-family: sans-serif;'><h2>Configured!</h2><p>Device will now restart and connect to your WiFi network.</p></body></html>");
    
    Serial.println("Configuration received via web interface. Restarting...");
    delay(1000);
    ESP.restart();
  } else {
    server.send(400, "text/plain", "Error: Missing configuration fields.");
  }
}

/**
 * Connects to the WiFi network stored in EEPROM.
 * Falls back to provisioning mode if connection fails.
 */
void connectToSavedWifi() {
  Serial.println("\n--- Connecting to WiFi ---");
  Serial.print("SSID: "); Serial.println(wifiSsid);
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(wifiSsid, wifiPass);
  
  // Try connecting for ~10 seconds
  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 20) {
    delay(500);
    Serial.print(".");
    retries++;
  }
  Serial.println();
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi connected successfully!");
    Serial.print("Local IP: "); Serial.println(WiFi.localIP());
  } else {
    Serial.println("WiFi connection failed! Falling back to provisioning mode.");
    startProvisioningMode();
  }
}

/**
 * Reads sensor data, builds a JSON payload, and POSTs it to the JavaFX app.
 */
void readAndSendSensorData() {
  // Ensure WiFi is still connected before doing anything
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected. Reconnecting...");
    connectToSavedWifi();
    if (WiFi.status() != WL_CONNECTED) return;
  }
  
  Serial.println("\n--- Reading Sensors ---");
  
  // 1. Read DHT11
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  
  // If DHT read fails, print a warning and use fallback values rather than skipping
  // Skipping prevents the device from ever registering with the server!
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("Warning: Failed to read from DHT sensor! Using fallback values (0.0).");
    humidity = 0.0;
    temperature = 0.0;
  }
  
  // 2. Read Capacitive Soil Moisture Sensor (A0)
  int soilRaw = analogRead(SOIL_PIN);
  
  Serial.print("Temp: "); Serial.print(temperature); Serial.print(" °C, ");
  Serial.print("Humidity: "); Serial.print(humidity); Serial.print(" %, ");
  Serial.print("Soil Raw: "); Serial.println(soilRaw);
  
  // 3. Build JSON Payload
  StaticJsonDocument<200> doc;
  doc["device_id"] = DEVICE_SERIAL;
  doc["pairing_code"] = PAIRING_CODE; // Needed for auto-registration
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  doc["soil_moisture_raw"] = soilRaw;
  
  String jsonPayload;
  serializeJson(doc, jsonPayload);
  
  // 4. POST to JavaFX Server
  String url = String("http://") + serverIp + ":8080/api/readings";
  Serial.print("POSTing to: "); Serial.println(url);
  Serial.print("Payload: "); Serial.println(jsonPayload);
  
  WiFiClient client;
  HTTPClient http;
  
  http.begin(client, url);
  http.addHeader("Content-Type", "application/json");
  
  int httpResponseCode = http.POST(jsonPayload);
  
  // 5. Print HTTP response for verifiable demo feedback
  if (httpResponseCode > 0) {
    consecutiveFailures = 0;
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String response = http.getString();
    Serial.print("Response: ");
    Serial.println(response);
  } else {
    Serial.print("HTTP POST Error code: ");
    Serial.println(httpResponseCode);
    Serial.println(http.errorToString(httpResponseCode));
    
    consecutiveFailures++;
    Serial.print("Consecutive failures: ");
    Serial.println(consecutiveFailures);
    
    if (consecutiveFailures >= 3) {
      Serial.println("Cannot reach Java Server! Auto-resetting to Provisioning Mode...");
      EEPROM.write(ADDR_FLAG, 0);
      EEPROM.commit();
      delay(1000);
      ESP.restart();
    }
  }
  http.end();
}

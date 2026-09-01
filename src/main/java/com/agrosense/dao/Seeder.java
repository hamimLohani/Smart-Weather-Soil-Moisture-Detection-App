package com.agrosense.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates all database tables and seeds sample data.
 * Safe to call on every startup — uses CREATE TABLE IF NOT EXISTS.
 */
public class Seeder {

    public static void initializeAndSeed() {
        createSchema();
        seedData();
    }

    private static void createSchema() {
        String[] ddl = {
            // Core entity tables
            """
            CREATE TABLE IF NOT EXISTS Customer (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                name       TEXT NOT NULL,
                phone      TEXT,
                email      TEXT NOT NULL UNIQUE,
                password_hash TEXT,
                salt          TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS Product (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                model_name  TEXT NOT NULL,
                description TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS Site (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id      INTEGER NOT NULL REFERENCES Customer(id),
                name             TEXT NOT NULL,
                use_case_profile TEXT NOT NULL CHECK(use_case_profile IN ('HOME','FARM')),
                created_at       DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS DeviceUnit (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                serial_number        TEXT NOT NULL UNIQUE,
                product_id           INTEGER NOT NULL REFERENCES Product(id),
                status               TEXT NOT NULL DEFAULT 'UNPAIRED'
                                         CHECK(status IN ('UNPAIRED','PAIRED','FAULTY')),
                dry_calibration_value INTEGER NOT NULL DEFAULT 820,
                wet_calibration_value INTEGER NOT NULL DEFAULT 380,
                last_seen            DATETIME
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS PairingCode (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                code           TEXT NOT NULL UNIQUE,
                device_unit_id INTEGER NOT NULL REFERENCES DeviceUnit(id),
                status         TEXT NOT NULL DEFAULT 'UNUSED'
                                   CHECK(status IN ('UNUSED','ACTIVE','EXPIRED','REVOKED')),
                generated_date DATE NOT NULL DEFAULT CURRENT_DATE,
                used_date      DATE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS DevicePairing (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                device_unit_id INTEGER NOT NULL REFERENCES DeviceUnit(id),
                customer_id    INTEGER NOT NULL REFERENCES Customer(id),
                site_id        INTEGER NOT NULL REFERENCES Site(id),
                paired_date    DATETIME DEFAULT CURRENT_TIMESTAMP,
                unpaired_date  DATETIME,
                is_active      INTEGER NOT NULL DEFAULT 1
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS SensorReading (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                device_unit_id INTEGER NOT NULL REFERENCES DeviceUnit(id),
                reading_type   TEXT NOT NULL CHECK(reading_type IN ('TEMPERATURE','HUMIDITY','SOIL_MOISTURE')),
                value          REAL NOT NULL,
                timestamp      DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,
            // Performance index for history/chart queries
            """
            CREATE INDEX IF NOT EXISTS idx_sensor_device_time
                ON SensorReading(device_unit_id, timestamp)
            """,
            """
            CREATE TABLE IF NOT EXISTS AlertRule (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                site_id             INTEGER NOT NULL REFERENCES Site(id),
                reading_type        TEXT NOT NULL,
                comparison_operator TEXT NOT NULL,
                threshold_value     REAL NOT NULL,
                is_active           INTEGER NOT NULL DEFAULT 1
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS AlertEvent (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                device_unit_id  INTEGER NOT NULL REFERENCES DeviceUnit(id),
                alert_rule_id   INTEGER NOT NULL REFERENCES AlertRule(id),
                triggered_value REAL NOT NULL,
                status          TEXT NOT NULL DEFAULT 'OPEN'
                                    CHECK(status IN ('OPEN','ACKNOWLEDGED','RESOLVED')),
                created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
                resolved_at     DATETIME
            )
            """
        };

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql.trim());
            }
            // Migrate existing schema — add password columns if missing (safe on repeated runs)
            try { st.execute("ALTER TABLE Customer ADD COLUMN password_hash TEXT"); } catch (Exception ignored) {}
            try { st.execute("ALTER TABLE Customer ADD COLUMN salt TEXT"); } catch (Exception ignored) {}
            System.out.println("[Seeder] Schema created successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    private static void seedData() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Only seed if tables are empty
            var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM Customer");
            if (rs.getInt(1) > 0) {
                System.out.println("[Seeder] Data already seeded — skipping.");
                return;
            }

            // Products
            var ps = conn.prepareStatement(
                "INSERT INTO Product(model_name, description) VALUES(?,?)");
            ps.setString(1, "AgroNode v1");
            ps.setString(2, "ESP8266 + DHT11 + Capacitive Soil Sensor + OLED (Gen 1)");
            ps.executeUpdate();
            ps.setString(1, "AgroNode v2");
            ps.setString(2, "ESP8266 + DHT22 + Capacitive Soil Sensor + OLED (Gen 2, higher accuracy)");
            ps.executeUpdate();
            ps.close();

            // Demo password: Demo@Agro#1
            final String DEMO_PASSWORD = "Demo@Agro#1";
            String salt1 = com.agrosense.service.PasswordUtils.generateSalt();
            String hash1 = com.agrosense.service.PasswordUtils.hashPassword(DEMO_PASSWORD, salt1);
            String salt2 = com.agrosense.service.PasswordUtils.generateSalt();
            String hash2 = com.agrosense.service.PasswordUtils.hashPassword(DEMO_PASSWORD, salt2);

            // Customers
            var psCust = conn.prepareStatement(
                "INSERT INTO Customer(name, phone, email, password_hash, salt) VALUES(?,?,?,?,?)");
            psCust.setString(1, "Alice Nguyen");
            psCust.setString(2, "+1-555-0101");
            psCust.setString(3, "alice@example.com");
            psCust.setString(4, hash1);
            psCust.setString(5, salt1);
            psCust.executeUpdate();
            psCust.setString(1, "Bob Mwangi");
            psCust.setString(2, "+254-712-000002");
            psCust.setString(3, "bob@example.com");
            psCust.setString(4, hash2);
            psCust.setString(5, salt2);
            psCust.executeUpdate();
            psCust.close();
            System.out.println("[Seeder] Demo password for all accounts: " + DEMO_PASSWORD);

            // Device Units (SM-0001, SM-0002 unpaired; SM-0003 faulty)
            var psDev = conn.prepareStatement(
                "INSERT INTO DeviceUnit(serial_number, product_id, status, " +
                "dry_calibration_value, wet_calibration_value) VALUES(?,?,?,?,?)");
            psDev.setString(1, "SM-0001"); psDev.setInt(2, 1);
            psDev.setString(3, "UNPAIRED"); psDev.setInt(4, 820); psDev.setInt(5, 380);
            psDev.executeUpdate();
            psDev.setString(1, "SM-0002"); psDev.setInt(2, 2);
            psDev.setString(3, "UNPAIRED"); psDev.setInt(4, 800); psDev.setInt(5, 360);
            psDev.executeUpdate();
            psDev.setString(1, "SM-0003"); psDev.setInt(2, 1);
            psDev.setString(3, "FAULTY"); psDev.setInt(4, 820); psDev.setInt(5, 380);
            psDev.executeUpdate();
            psDev.close();

            // Pairing Codes
            var psCode = conn.prepareStatement(
                "INSERT INTO PairingCode(code, device_unit_id, status, generated_date) VALUES(?,?,?,?)");
            psCode.setString(1, "AGRO-1A2B-3C4D"); psCode.setInt(2, 1);
            psCode.setString(3, "UNUSED"); psCode.setString(4, "2026-08-01");
            psCode.executeUpdate();
            psCode.setString(1, "AGRO-5E6F-7G8H"); psCode.setInt(2, 2);
            psCode.setString(3, "UNUSED"); psCode.setString(4, "2026-08-15");
            psCode.executeUpdate();
            // Expired code for SM-0003 (generated 2 years ago)
            psCode.setString(1, "AGRO-DEAD-FAUL"); psCode.setInt(2, 3);
            psCode.setString(3, "EXPIRED"); psCode.setString(4, "2024-01-01");
            psCode.executeUpdate();
            psCode.close();

            System.out.println("[Seeder] Sample data inserted.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed data", e);
        }
    }
}
